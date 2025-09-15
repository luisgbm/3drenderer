#include <math.h>
#include "clipping.h"

#define NUM_PLANES 6
plane_t frustum_planes[NUM_PLANES];

void init_frustum_planes(float fovx, float fovy, float z_near, float z_far) {
	float cos_half_fovy = cos(fovy / 2);
	float sin_half_fovy = sin(fovy / 2);

	float cos_half_fovx = cos(fovx / 2);
	float sin_half_fovx = sin(fovx / 2);

	frustum_planes[LEFT_FRUSTUM_PLANE].point = vec3_new(0, 0, 0);
	frustum_planes[LEFT_FRUSTUM_PLANE].normal.x = cos_half_fovx;
	frustum_planes[LEFT_FRUSTUM_PLANE].normal.y = 0;
	frustum_planes[LEFT_FRUSTUM_PLANE].normal.z = sin_half_fovx;

	frustum_planes[RIGHT_FRUSTUM_PLANE].point = vec3_new(0, 0, 0);
	frustum_planes[RIGHT_FRUSTUM_PLANE].normal.x = -cos_half_fovx;
	frustum_planes[RIGHT_FRUSTUM_PLANE].normal.y = 0;
	frustum_planes[RIGHT_FRUSTUM_PLANE].normal.z = sin_half_fovx;

	frustum_planes[TOP_FRUSTUM_PLANE].point = vec3_new(0, 0, 0);
	frustum_planes[TOP_FRUSTUM_PLANE].normal.x = 0;
	frustum_planes[TOP_FRUSTUM_PLANE].normal.y = -cos_half_fovy;
	frustum_planes[TOP_FRUSTUM_PLANE].normal.z = sin_half_fovy;

	frustum_planes[BOTTOM_FRUSTUM_PLANE].point = vec3_new(0, 0, 0);
	frustum_planes[BOTTOM_FRUSTUM_PLANE].normal.x = 0;
	frustum_planes[BOTTOM_FRUSTUM_PLANE].normal.y = cos_half_fovy;
	frustum_planes[BOTTOM_FRUSTUM_PLANE].normal.z = sin_half_fovy;

	frustum_planes[NEAR_FRUSTUM_PLANE].point = vec3_new(0, 0, z_near);
	frustum_planes[NEAR_FRUSTUM_PLANE].normal.x = 0;
	frustum_planes[NEAR_FRUSTUM_PLANE].normal.y = 0;
	frustum_planes[NEAR_FRUSTUM_PLANE].normal.z = 1;

	frustum_planes[FAR_FRUSTUM_PLANE].point = vec3_new(0, 0, z_far);
	frustum_planes[FAR_FRUSTUM_PLANE].normal.x = 0;
	frustum_planes[FAR_FRUSTUM_PLANE].normal.y = 0;
	frustum_planes[FAR_FRUSTUM_PLANE].normal.z = -1;
}

void create_triangles_from_polygon(polygon_t* polygon, triangle_t* triangles_after_clipping, int* num_triangles_after_clipping) {
	for (int i = 0; i < polygon->num_vertices - 2; i++) {
		int index0 = 0;
		int index1 = i + 1;
		int index2 = i + 2;

		triangles_after_clipping[i].points[0] = vec4_from_vec3(polygon->vertices[index0]);
		triangles_after_clipping[i].points[1] = vec4_from_vec3(polygon->vertices[index1]);
		triangles_after_clipping[i].points[2] = vec4_from_vec3(polygon->vertices[index2]);

		triangles_after_clipping[i].texcoords[0] = polygon->texcoords[index0];
		triangles_after_clipping[i].texcoords[1] = polygon->texcoords[index1];
		triangles_after_clipping[i].texcoords[2] = polygon->texcoords[index2];
	}

	*num_triangles_after_clipping = polygon->num_vertices - 2;
}

polygon_t create_polygon_from_triangle(vec3_t v0, vec3_t v1, vec3_t v2, tex2_t t0, tex2_t t1, tex2_t t2) {
	polygon_t polygon = {
		.vertices = { v0, v1, v2 },
		.num_vertices = 3,
		.texcoords = { t0, t1, t2 }
	};

	return polygon;
}

float float_lerp(float a, float b, float t) {
	return a + (b - a) * t;
}

void clip_polygon_against_plane(polygon_t* polygon, int plane) {
	vec3_t plane_point = frustum_planes[plane].point;
	vec3_t plane_normal = frustum_planes[plane].normal;

	vec3_t inside_vertices[MAX_NUM_POLY_VERTICES];
	tex2_t inside_texcoords[MAX_NUM_POLY_VERTICES];
	int num_inside_vertices = 0;

	int current_index = 0;
	vec3_t* current_vertex = &polygon->vertices[current_index];
	tex2_t* current_texcoords = &polygon->texcoords[current_index];

	vec3_t* previous_vertex = &polygon->vertices[polygon->num_vertices - 1];
	tex2_t* previous_texcoords = &polygon->texcoords[polygon->num_vertices - 1];

	float current_dot = 0;
	float previous_dot = vec3_dot(vec3_sub(*previous_vertex, plane_point), plane_normal);

	for (int i = 0; i < polygon->num_vertices; i++) {
		current_dot = vec3_dot(vec3_sub(*current_vertex, plane_point), plane_normal);

		if (current_dot * previous_dot < 0) {
			float t = previous_dot / (previous_dot - current_dot);

			vec3_t intersection_point = {
				.x = float_lerp(previous_vertex->x, current_vertex->x, t),
				.y = float_lerp(previous_vertex->y, current_vertex->y, t),
				.z = float_lerp(previous_vertex->z, current_vertex->z, t)
			};

			tex2_t interpolated_texcoords = {
				.u = float_lerp(previous_texcoords->u, current_texcoords->u, t),
				.v = float_lerp(previous_texcoords->v, current_texcoords->v, t)
			};

			inside_vertices[num_inside_vertices] = vec3_clone(&intersection_point);
			inside_texcoords[num_inside_vertices] = tex2_clone(&interpolated_texcoords);

			num_inside_vertices++;
		}

		if (current_dot > 0) {
			inside_vertices[num_inside_vertices] = vec3_clone(current_vertex);
			inside_texcoords[num_inside_vertices] = tex2_clone(current_texcoords);
			num_inside_vertices++;
		}

		current_index++;

		previous_dot = current_dot;

		previous_vertex = current_vertex;
		previous_texcoords = current_texcoords;

		current_texcoords = &polygon->texcoords[current_index];
		current_vertex = &polygon->vertices[current_index];
	}

	for (int i = 0; i < num_inside_vertices; i++) {
		polygon->vertices[i] = inside_vertices[i];
		polygon->texcoords[i] = inside_texcoords[i];
	}

	polygon->num_vertices = num_inside_vertices;
}

void clip_polygon(polygon_t* polygon) {
	clip_polygon_against_plane(polygon, LEFT_FRUSTUM_PLANE);
	clip_polygon_against_plane(polygon, RIGHT_FRUSTUM_PLANE);
	clip_polygon_against_plane(polygon, TOP_FRUSTUM_PLANE);
	clip_polygon_against_plane(polygon, BOTTOM_FRUSTUM_PLANE);
	clip_polygon_against_plane(polygon, NEAR_FRUSTUM_PLANE);
	clip_polygon_against_plane(polygon, FAR_FRUSTUM_PLANE);
}
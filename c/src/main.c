#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <SDL2/SDL.h>
#include "upng.h"
#include "array.h"
#include "display.h"
#include "clipping.h"
#include "vector.h"
#include "matrix.h"
#include "light.h"
#include "camera.h"
#include "triangle.h"
#include "texture.h"
#include "mesh.h"

#define M_PI 3.14159265358979323846

bool is_running = false;
int previous_frame_time = 0;
float delta_time = 0; 

#define MAX_TRIANGLES 10000
triangle_t triangles_to_render[MAX_TRIANGLES];
int num_triangles_to_render = 0;

mat4_t proj_matrix;

void setup(void) {
    set_render_method(RENDER_TEXTURED);
    set_cull_method(CULL_BACKFACE);

    init_light(vec3_new(0, 0, 1));
    init_camera(vec3_new(0, 0, 0), vec3_new(0, 0, 1));

    float aspecty = (float)get_window_height() / (float)get_window_width();
    float aspectx = (float)get_window_width() / (float)get_window_height();

    float fovy = 3.141592 / 3.0; // the same as 180/3, or 60deg
    float fovx = 2.0 * atan(tan(fovy / 2) * aspectx);
    
    float z_near = 1.0;
    float z_far = 20.0;
    proj_matrix = mat4_make_perspective(fovy, aspecty, z_near, z_far);

    init_frustum_planes(fovx, fovy, z_near, z_far);

    load_mesh("./assets/runway.obj", "./assets/runway.png", vec3_new(1, 1, 1), vec3_new(0, -1.5, +23), vec3_new(0, 0, 0));
    load_mesh("./assets/f22.obj", "./assets/f22.png", vec3_new(1, 1, 1), vec3_new(0, -1.3, +5), vec3_new(0, -M_PI/2, 0));
    load_mesh("./assets/efa.obj", "./assets/efa.png", vec3_new(1, 1, 1), vec3_new(-2, -1.3, +9), vec3_new(0, -M_PI/2, 0));
    load_mesh("./assets/f117.obj", "./assets/f117.png", vec3_new(1, 1, 1), vec3_new(+2, -1.3, +9), vec3_new(0, -M_PI/2, 0));
}

void process_input(void) {
    SDL_Event event;
    while(SDL_PollEvent(&event)) {
        switch (event.type) {
        case SDL_QUIT:
            is_running = false;
            break;
        case SDL_KEYDOWN:
            if (event.key.keysym.sym == SDLK_ESCAPE) {
                is_running = false;
                break;
            }
            
            if (event.key.keysym.sym == SDLK_1){
                set_render_method(RENDER_WIRE_VERTEX);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_2) {
                set_render_method(RENDER_WIRE);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_3) {
                set_render_method(RENDER_FILL_TRIANGLE);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_4) {
                set_render_method(RENDER_FILL_TRIANGLE_WIRE);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_5) {
                set_render_method(RENDER_TEXTURED);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_6) {
                set_render_method(RENDER_TEXTURED_WIRE);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_c) {
                set_cull_method(CULL_BACKFACE);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_x) {
                set_cull_method(CULL_NONE);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_UP) {
                rotate_camera_pitch(3.0 * delta_time);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_DOWN) {
                rotate_camera_pitch(-3.0 * delta_time);
                break;
            }

            if (event.key.keysym.sym == SDLK_RIGHT) {
                rotate_camera_yaw(1.0 * delta_time);
                break;
            }

            if (event.key.keysym.sym == SDLK_LEFT) {
                rotate_camera_yaw(-1.0 * delta_time);
                break;
            }
            
            if (event.key.keysym.sym == SDLK_w) {
                update_camera_forward_velocity(vec3_mul(get_camera_direction(), 5.0 * delta_time));
                update_camera_position(vec3_add(get_camera_position(), get_camera_forward_velocity()));
                break;
            }

            if (event.key.keysym.sym == SDLK_s) {
                update_camera_forward_velocity(vec3_mul(get_camera_direction(), 5.0 * delta_time));
                update_camera_position(vec3_sub(get_camera_position(), get_camera_forward_velocity()));
                break;
            }

            break;
        }
    }
}

// Model space -> World space -> Camera space -> Clipping -> Projection -> Image space -> Screen space
void process_graphics_pipeline_stages(mesh_t* mesh) {    
    //mesh.rotation.x += 0.0 * delta_time;
    //mesh.rotation.y += 0.0 * delta_time;
    //mesh.rotation.z += 0.0 * delta_time;
    //mesh.translation.z = 5.0;

    vec3_t target = get_camera_lookat_target();;
    
    vec3_t up_direction = { 0, 1, 0 };
    
    mat4_t view_matrix = mat4_look_at(get_camera_position(), target, up_direction);

    mat4_t scale_matrix = mat4_make_scale(mesh->scale.x, mesh->scale.y, mesh->scale.z);
    mat4_t translation_matrix = mat4_make_translation(mesh->translation.x, mesh->translation.y, mesh->translation.z);
    mat4_t rotation_matrix_x = mat4_make_rotation_x(mesh->rotation.x);
    mat4_t rotation_matrix_y = mat4_make_rotation_y(mesh->rotation.y);
    mat4_t rotation_matrix_z = mat4_make_rotation_z(mesh->rotation.z);

    int num_faces = array_length(mesh->faces);
    for (int i = 0; i < num_faces; i++) {
        face_t mesh_face = mesh->faces[i];

        vec3_t face_vertices[3];
        face_vertices[0] = mesh->vertices[mesh_face.a - 1];
        face_vertices[1] = mesh->vertices[mesh_face.b - 1];
        face_vertices[2] = mesh->vertices[mesh_face.c - 1];

        vec4_t transformed_vertices[3];

        for (int j = 0; j < 3; j++) {
            vec4_t transformed_vertex = vec4_from_vec3(face_vertices[j]);

            mat4_t world_matrix = mat4_identity();

            world_matrix = mat4_mul_mat4(scale_matrix, world_matrix);
            world_matrix = mat4_mul_mat4(rotation_matrix_z, world_matrix);
            world_matrix = mat4_mul_mat4(rotation_matrix_y, world_matrix);
            world_matrix = mat4_mul_mat4(rotation_matrix_x, world_matrix);
            world_matrix = mat4_mul_mat4(translation_matrix, world_matrix);

            transformed_vertex = mat4_mul_vec4(world_matrix, transformed_vertex);

            transformed_vertex = mat4_mul_vec4(view_matrix, transformed_vertex);

            transformed_vertices[j] = transformed_vertex;
        }

        vec3_t face_normal = get_triangle_normal(transformed_vertices);

        if (is_cull_backface()) {
            vec3_t camera_ray = vec3_sub(vec3_new(0, 0, 0), vec3_from_vec4(transformed_vertices[0]));

            float dot_normal_camera = vec3_dot(face_normal, camera_ray);

            if (dot_normal_camera < 0) {
                continue;
            }
        }

        polygon_t polygon = create_polygon_from_triangle(
            vec3_from_vec4(transformed_vertices[0]),
            vec3_from_vec4(transformed_vertices[1]),
            vec3_from_vec4(transformed_vertices[2]),
            mesh_face.a_uv,
            mesh_face.b_uv,
            mesh_face.c_uv
        );

        clip_polygon(&polygon);

        triangle_t triangles_after_clipping[MAX_NUM_POLY_TRIANGLES];
        int num_triangles_after_clipping = 0;

        create_triangles_from_polygon(&polygon, triangles_after_clipping, &num_triangles_after_clipping);

        for (int t = 0; t < num_triangles_after_clipping; t++) {
            triangle_t triangle_after_clipping = triangles_after_clipping[t];

            vec4_t projected_points[3];

            for (int j = 0; j < 3; j++) {
                projected_points[j] = mat4_mul_vec4(proj_matrix, triangle_after_clipping.points[j]);

                if (projected_points[j].w != 0) {
                    projected_points[j].x /= projected_points[j].w;
                    projected_points[j].y /= projected_points[j].w;
                    projected_points[j].z /= projected_points[j].w;
                }

                projected_points[j].y *= -1;

                projected_points[j].x *= (get_window_width() / 2.0);
                projected_points[j].y *= (get_window_height() / 2.0);

                projected_points[j].x += (get_window_width() / 2.0);
                projected_points[j].y += (get_window_height() / 2.0);
            }

            float light_intensity_factor = -vec3_dot(face_normal, get_light_direction());

            uint32_t triangle_color = light_apply_intensity(mesh_face.color, light_intensity_factor);

            triangle_t triangle_to_render = {
                .points = {
                    { projected_points[0].x, projected_points[0].y, projected_points[0].z, projected_points[0].w },
                    { projected_points[1].x, projected_points[1].y, projected_points[1].z, projected_points[1].w },
                    { projected_points[2].x, projected_points[2].y, projected_points[2].z, projected_points[2].w },
                },
                .texcoords = {
                    { triangle_after_clipping.texcoords[0].u, triangle_after_clipping.texcoords[0].v },
                    { triangle_after_clipping.texcoords[1].u, triangle_after_clipping.texcoords[1].v },
                    { triangle_after_clipping.texcoords[2].u, triangle_after_clipping.texcoords[2].v }
                },
                .color = triangle_color,
                .texture = mesh->texture
            };

            if (num_triangles_to_render < MAX_TRIANGLES) {
                triangles_to_render[num_triangles_to_render++] = triangle_to_render;
            }
        }
    }
}

void update(void) {
    int time_to_wait = FRAME_TARGET_TIME - (SDL_GetTicks() - previous_frame_time);

    if (time_to_wait > 0 && time_to_wait <= FRAME_TARGET_TIME) {
        SDL_Delay(time_to_wait);
    }

    delta_time = (SDL_GetTicks() - previous_frame_time) / 1000.0;

    previous_frame_time = SDL_GetTicks();

    num_triangles_to_render = 0;

    for (int mesh_index = 0; mesh_index < get_number_meshes(); mesh_index++) {
        mesh_t* mesh = get_mesh(mesh_index);

        process_graphics_pipeline_stages(mesh);
    }
}

void render(void) {
    clear_color_buffer(0xFF000000);
    clear_z_buffer();

    draw_grid();

    for (int i = 0; i < num_triangles_to_render; i++) {
        triangle_t triangle = triangles_to_render[i];

        if (should_render_filled_triangles()) {
            draw_filled_triangle(
                triangle.points[0].x, triangle.points[0].y, triangle.points[0].z, triangle.points[0].w, // vertex A
                triangle.points[1].x, triangle.points[1].y, triangle.points[1].z, triangle.points[1].w, // vertex B
                triangle.points[2].x, triangle.points[2].y, triangle.points[2].z, triangle.points[2].w, // vertex C
                triangle.color
            );
        }

        if (should_render_textured_triangles()) {
            draw_textured_triangle(
                triangle.points[0].x, triangle.points[0].y, triangle.points[0].z, triangle.points[0].w, triangle.texcoords[0].u, triangle.texcoords[0].v, // vertex A
                triangle.points[1].x, triangle.points[1].y, triangle.points[1].z, triangle.points[1].w, triangle.texcoords[1].u, triangle.texcoords[1].v, // vertex B
                triangle.points[2].x, triangle.points[2].y, triangle.points[2].z, triangle.points[2].w, triangle.texcoords[2].u, triangle.texcoords[2].v, // vertex C
                triangle.texture
            );
        }

        if (should_render_wireframe()) {
            draw_triangle(
                triangle.points[0].x, triangle.points[0].y, // vertex A
                triangle.points[1].x, triangle.points[1].y, // vertex B
                triangle.points[2].x, triangle.points[2].y, // vertex C
                0xFFFFFFFF
            );
        }

        if (should_render_vertex()) {
            draw_rect(triangle.points[0].x - 3, triangle.points[0].y - 3, 6, 6, 0xFF0000FF); // vertex A
            draw_rect(triangle.points[1].x - 3, triangle.points[1].y - 3, 6, 6, 0xFF0000FF); // vertex B
            draw_rect(triangle.points[2].x - 3, triangle.points[2].y - 3, 6, 6, 0xFF0000FF); // vertex C
        }
    }

    render_color_buffer();
}

void free_resources(void) {
    free_meshes();
    destroy_window();
}

int main(void) {
    is_running = initialize_window();

    setup();

    while (is_running) {
        process_input();
        update();
        render();
    }

    free_resources();

    return 0;
}

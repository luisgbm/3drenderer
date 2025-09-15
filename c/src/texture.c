#include "texture.h"

tex2_t tex2_clone(tex2_t* tex) {
    tex2_t new_tex;
    new_tex.u = tex->u;
    new_tex.v = tex->v;
    return new_tex;
}
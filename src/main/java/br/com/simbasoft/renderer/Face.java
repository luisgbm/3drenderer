package br.com.simbasoft.renderer;

public record Face(
        int a,
        int b,
        int c,
        Texture auv,
        Texture buv,
        Texture cuv,
        int color) {
}

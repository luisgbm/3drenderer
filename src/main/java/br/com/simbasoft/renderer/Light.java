package br.com.simbasoft.renderer;

public record Light(Vector3 direction) {
    public static int applyIntensity(int originalColor, double percentageFactor) {
        if (percentageFactor < 0) percentageFactor = 0;
        if (percentageFactor > 1) percentageFactor = 1;

        int a = (originalColor & 0xFF000000);
        int r = (int) ((originalColor & 0x00FF0000) * percentageFactor);
        int g = (int) (((originalColor & 0x0000FF00)) * percentageFactor);
        int b = (int) (((originalColor & 0x000000FF)) * percentageFactor);

        return a | (r & 0x00FF0000) | (g & 0x0000FF00) | (b & 0x000000FF);
    }
}

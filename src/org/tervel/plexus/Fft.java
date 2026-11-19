package org.tervel.plexus;

/**
 * A plain radix-2 fast Fourier transform, one- and two-dimensional, so Plexus needs nothing beyond the JDK.
 * Lengths must be powers of two; a caller pads to {@link #pow2}.
 */
final class Fft {

    private Fft() { }

    /** The smallest power of two at least {@code n}. */
    static int pow2(int n) {
        var p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    /** Transform {@code re}/{@code im} in place; the inverse is scaled by {@code 1/n}. */
    static void transform(double[] re, double[] im, boolean inverse) {
        final var n = re.length;
        for (int i = 1, j = 0; i < n; i++) {               // bit reversal
            var bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        for (var len = 2; len <= n; len <<= 1) {
            final var angle = 2 * Math.PI / len * (inverse ? 1 : -1);
            final var wr = Math.cos(angle);
            final var wi = Math.sin(angle);
            for (var i = 0; i < n; i += len) {
                double cr = 1, ci = 0;
                for (var k = 0; k < len / 2; k++) {
                    final var a = i + k;
                    final var b = a + len / 2;
                    final var xr = re[b] * cr - im[b] * ci;
                    final var xi = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - xr; im[b] = im[a] - xi;
                    re[a] += xr; im[a] += xi;
                    final var nr = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = nr;
                }
            }
        }
        if (inverse) for (var i = 0; i < n; i++) { re[i] /= n; im[i] /= n; }
    }

    /** Transform a {@code [h][w]} array in place, rows then columns; both powers of two. */
    static void transform(double[][] re, double[][] im, boolean inverse) {
        final var h = re.length;
        final var w = re[0].length;
        for (var y = 0; y < h; y++) transform(re[y], im[y], inverse);
        final var cr = new double[h];
        final var ci = new double[h];
        for (var x = 0; x < w; x++) {
            for (var y = 0; y < h; y++) { cr[y] = re[y][x]; ci[y] = im[y][x]; }
            transform(cr, ci, inverse);
            for (var y = 0; y < h; y++) { re[y][x] = cr[y]; im[y][x] = ci[y]; }
        }
    }
}

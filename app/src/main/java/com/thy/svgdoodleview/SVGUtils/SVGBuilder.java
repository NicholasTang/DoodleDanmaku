package com.thy.svgdoodleview.SVGUtils;

import android.graphics.ColorFilter;
import android.util.Log;

import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.thy.svgdoodleview.SVGUtils.SVGParser.SVGHandler;

public class SVGBuilder {
    private InputStream data;
    private Integer searchColor = null;
    private Integer replaceColor = null;
    private ColorFilter strokeColorFilter = null, fillColorFilter = null;
    private boolean whiteMode = false;
    private boolean overideOpacity = false;
    private boolean closeInputStream = true;

    /**
     * Parse SVG data from a string.
     *
     * @param svgData the string containing SVG XML data.
     */
    public SVGBuilder readFromString(String svgData) {
        this.data = new ByteArrayInputStream(svgData.getBytes());
        return this;
    }

    /**
     * Loads, reads, parses the SVG (or SVGZ).
     *
     * @return the parsed SVG.
     * @throws SVGParseException if there is an error while parsing.
     */
    public SVG build(float canvasWidth, float canvasHeight) throws SVGParseException {
        if (data == null) {
            throw new IllegalStateException("SVG input not specified. Call one of the readFrom...() methods first.");
        }

        try {
            final SVGHandler handler = new SVGHandler(canvasWidth,canvasHeight);

            // SVGZ support (based on https://github.com/josefpavlik/svg-android/commit/fc0522b2e1):
            if(!data.markSupported())
                data = new BufferedInputStream(data); // decorate stream so we can use mark/reset
            try {
                data.mark(4);
                byte[] magic = new byte[2];
                int r = data.read(magic, 0, 2);
                int magicInt = (magic[0] + ((magic[1]) << 8)) & 0xffff;
                data.reset();
                if (r == 2 && magicInt == GZIPInputStream.GZIP_MAGIC) {
                    // Log.d(SVGParser.TAG, "SVG is gzipped");
                    GZIPInputStream gin = new GZIPInputStream(data);
                    data = gin;
                }
            } catch (IOException ioe) {
                throw new SVGParseException(ioe);
            }

            final SVG svg = SVGParser.parse(new InputSource(data), handler);
            return svg;

        } finally {
            if (closeInputStream) {
                try {
                    data.close();
                } catch (IOException e) {
                    Log.e(SVGParser.TAG, "Error closing SVG input stream.", e);
                }
            }
        }
    }
}

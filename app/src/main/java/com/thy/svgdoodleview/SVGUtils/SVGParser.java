package com.thy.svgdoodleview.SVGUtils;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

import com.thy.svgdoodleview.Utils.LogHelper;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * @author Tangyan
 */
public class SVGParser {

    static final String TAG = "SVGAndroid";

    private static boolean DISALLOW_DOCTYPE_DECL = true;

    static SVG parse(InputSource data, SVGHandler handler) throws SVGParseException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(handler);
            xr.setFeature("http://xml.org/sax/features/validation", false);
            if (DISALLOW_DOCTYPE_DECL) {
                try {
                    xr.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                } catch (SAXNotRecognizedException e) {
                    DISALLOW_DOCTYPE_DECL = false;
                }
            }
            xr.parse(data);

            SVG result = new SVG(handler.pathList, handler.colorList, handler.strokeWidthList);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse SVG.", e);
            throw new SVGParseException(e);
        }
    }

    /**
     * This is where the hard-to-parse paths are handled. Uppercase rules are absolute positions, lowercase are
     * relative. Types of path rules:
     * <p/>
     * <ol>
     * <li>M/m - (x y)+ - Move to (without drawing)
     * <li>Z/z - (no params) - Close path (back to starting point)
     * <li>L/l - (x y)+ - Line to
     * <li>H/h - x+ - Horizontal ine to
     * <li>V/v - y+ - Vertical line to
     * <li>C/c - (x1 y1 x2 y2 x y)+ - Cubic bezier to
     * <li>S/s - (x2 y2 x y)+ - Smooth cubic bezier to (shorthand that assumes the x2, y2 from previous C/S is the x1,
     * y1 of this bezier)
     * <li>Q/q - (x1 y1 x y)+ - Quadratic bezier to
     * <li>T/t - (x y)+ - Smooth quadratic bezier to (assumes previous control point is "reflection" of last one w.r.t.
     * to current point)
     * </ol>
     * <p/>
     * Numbers are separate by whitespace, comma or nothing at all (!) if they are self-delimiting, (ie. begin with a -
     * sign)
     *
     * @param s the path string from the XML
     */
    private static Path doPath(String s, float widthScale, float heightScale) {
        LogHelper.warn("xxxxx", "doPath string:" + s);
        int n = s.length();
        ParserHelper ph = new ParserHelper(s, 0);
        ph.skipWhitespace();
        Path p = new Path();
        float lastX = 0;
        float lastY = 0;
        float lastX1 = 0;
        float lastY1 = 0;
        float subPathStartX = 0;
        float subPathStartY = 0;
        char prevCmd = 0;
        while (ph.pos < n) {
            char cmd = s.charAt(ph.pos);
            switch (cmd) {
                case '-':
                case '+':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (prevCmd == 'm' || prevCmd == 'M') {
                        cmd = (char) ((prevCmd) - 1);
                        break;
                    } else if (("lhvcsqta").indexOf(Character.toLowerCase(prevCmd)) >= 0) {
                        cmd = prevCmd;
                        break;
                    }
                default: {
                    ph.advance();
                    prevCmd = cmd;
                }
            }

            boolean wasCurve = false;
            switch (cmd) {
                case 'M':
                case 'm': {
                    float x = ph.nextFloat() * widthScale;
                    float y = ph.nextFloat() * heightScale;
                    if (cmd == 'm') {
                        subPathStartX += x;
                        subPathStartY += y;
                        p.rMoveTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        subPathStartX = x;
                        subPathStartY = y;
                        p.moveTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'Z':
                case 'z': {
                    p.close();
                    p.moveTo(subPathStartX, subPathStartY);
                    lastX = subPathStartX;
                    lastY = subPathStartY;
                    lastX1 = subPathStartX;
                    lastY1 = subPathStartY;
                    wasCurve = true;
                    break;
                }
                case 'T':
                case 't':
                    // todo - smooth quadratic Bezier (two parameters)
                case 'L':
                case 'l': {
                    float x = ph.nextFloat() * widthScale;
                    float y = ph.nextFloat() * heightScale;
                    if (cmd == 'l') {
                        p.rLineTo(x, y);
                        lastX += x;
                        lastY += y;
                    } else {
                        p.lineTo(x, y);
                        lastX = x;
                        lastY = y;
                    }
                    break;
                }
                case 'H':
                case 'h': {
                    float x = ph.nextFloat() * widthScale;
                    if (cmd == 'h') {
                        p.rLineTo(x, 0);
                        lastX += x;
                    } else {
                        p.lineTo(x, lastY);
                        lastX = x;
                    }
                    break;
                }
                case 'V':
                case 'v': {
                    float y = ph.nextFloat();
                    if (cmd == 'v') {
                        p.rLineTo(0, y);
                        lastY += y;
                    } else {
                        p.lineTo(lastX, y);
                        lastY = y;
                    }
                    break;
                }
                case 'C':
                case 'c': {
                    wasCurve = true;
                    float x1 = ph.nextFloat() * widthScale;
                    float y1 = ph.nextFloat() * heightScale;
                    float x2 = ph.nextFloat() * widthScale;
                    float y2 = ph.nextFloat() * heightScale;
                    float x = ph.nextFloat() * widthScale;
                    float y = ph.nextFloat() * heightScale;
                    if (cmd == 'c') {
                        x1 += lastX;
                        x2 += lastX;
                        x += lastX;
                        y1 += lastY;
                        y2 += lastY;
                        y += lastY;
                    }
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'Q':
                case 'q': {
                    // todo - quadratic Bezier (four parameters)
                    wasCurve = true;
                    float x1 = ph.nextFloat() * widthScale;
                    float y1 = ph.nextFloat() * heightScale;
                    float x2 = ph.nextFloat() * widthScale;
                    float y2 = ph.nextFloat() * heightScale;
                    if (cmd == 'q') {
                        x1 += lastX;
                        x2 += lastX;
                        y1 += lastY;
                        y2 += lastY;
                    }
                    p.quadTo(x1, y1, x2, y2);
                    lastX1 = x1;
                    lastY1 = y1;
                    lastX = x2;
                    lastY = y2;
                    break;
                }
                case 'S':
                case 's': {
                    wasCurve = true;
                    float x2 = ph.nextFloat() * widthScale;
                    float y2 = ph.nextFloat() * heightScale;
                    float x = ph.nextFloat() * widthScale;
                    float y = ph.nextFloat() * heightScale;
                    if (Character.isLowerCase(cmd)) {
                        x2 += lastX;
                        x += lastX;
                        y2 += lastY;
                        y += lastY;
                    }
                    float x1 = 2 * lastX - lastX1;
                    float y1 = 2 * lastY - lastY1;
                    p.cubicTo(x1, y1, x2, y2, x, y);
                    lastX1 = x2;
                    lastY1 = y2;
                    lastX = x;
                    lastY = y;
                    break;
                }
                case 'A':
                case 'a': {
                    float rx = ph.nextFloat();
                    float ry = ph.nextFloat();
                    float theta = ph.nextFloat();
                    int largeArc = ph.nextFlag();
                    int sweepArc = ph.nextFlag();
                    float x = ph.nextFloat();
                    float y = ph.nextFloat();
                    if (cmd == 'a') {
                        x += lastX;
                        y += lastY;
                    }
                    drawArc(p, lastX, lastY, x, y, rx, ry, theta, largeArc, sweepArc);
                    lastX = x;
                    lastY = y;
                    break;
                }
                default:
                    Log.w(TAG, "Invalid path command: " + cmd);
                    ph.advance();
            }
            if (!wasCurve) {
                lastX1 = lastX;
                lastY1 = lastY;
            }
            ph.skipWhitespace();
        }
        return p;
    }

    private static float angle(float x1, float y1, float x2, float y2) {
        return (float) Math.toDegrees(Math.atan2(x1, y1) - Math.atan2(x2, y2)) % 360;
    }

    private static final RectF arcRectf = new RectF();
    private static final Matrix arcMatrix = new Matrix();
    private static final Matrix arcMatrix2 = new Matrix();

    private static void drawArc(Path p, float lastX, float lastY, float x, float y, float rx, float ry, float theta,
                                int largeArc, int sweepArc) {
        if (rx == 0 || ry == 0) {
            p.lineTo(x, y);
            return;
        }

        if (x == lastX && y == lastY) {
            return; // nothing to draw
        }

        rx = Math.abs(rx);
        ry = Math.abs(ry);

        final float thrad = theta * (float) Math.PI / 180;
        final float st = (float) Math.sin(thrad);
        final float ct = (float) Math.cos(thrad);

        final float xc = (lastX - x) / 2;
        final float yc = (lastY - y) / 2;
        final float x1t = ct * xc + st * yc;
        final float y1t = -st * xc + ct * yc;

        final float x1ts = x1t * x1t;
        final float y1ts = y1t * y1t;
        float rxs = rx * rx;
        float rys = ry * ry;

        float lambda = (x1ts / rxs + y1ts / rys) * 1.001f; // add 0.1% to be sure that no out of range occurs due to
        // limited precision
        if (lambda > 1) {
            float lambdasr = (float) Math.sqrt(lambda);
            rx *= lambdasr;
            ry *= lambdasr;
            rxs = rx * rx;
            rys = ry * ry;
        }

        final float R =
                (float) (Math.sqrt((rxs * rys - rxs * y1ts - rys * x1ts) / (rxs * y1ts + rys * x1ts))
                        * ((largeArc == sweepArc) ? -1 : 1));
        final float cxt = R * rx * y1t / ry;
        final float cyt = -R * ry * x1t / rx;
        final float cx = ct * cxt - st * cyt + (lastX + x) / 2;
        final float cy = st * cxt + ct * cyt + (lastY + y) / 2;

        final float th1 = angle(1, 0, (x1t - cxt) / rx, (y1t - cyt) / ry);
        float dth = angle((x1t - cxt) / rx, (y1t - cyt) / ry, (-x1t - cxt) / rx, (-y1t - cyt) / ry);

        if (sweepArc == 0 && dth > 0) {
            dth -= 360;
        } else if (sweepArc != 0 && dth < 0) {
            dth += 360;
        }

        // draw
        if ((theta % 360) == 0) {
            // no rotate and translate need
            arcRectf.set(cx - rx, cy - ry, cx + rx, cy + ry);
            p.arcTo(arcRectf, th1, dth);
        } else {
            // this is the hard and slow part :-)
            arcRectf.set(-rx, -ry, rx, ry);

            arcMatrix.reset();
            arcMatrix.postRotate(theta);
            arcMatrix.postTranslate(cx, cy);
            arcMatrix.invert(arcMatrix2);

            p.transform(arcMatrix2);
            p.arcTo(arcRectf, th1, dth);
            p.transform(arcMatrix);
        }
    }

    private static String getStringAttr(String name, Attributes attributes) {
        int n = attributes.getLength();
        for (int i = 0; i < n; i++) {
            if (attributes.getLocalName(i).equals(name)) {
                return attributes.getValue(i);
            }
        }
        return null;
    }

    private static Float getFloatAttr(String name, Attributes attributes) {
        return getFloatAttr(name, attributes, null);
    }

    private static Float getFloatAttr(String name, Attributes attributes, Float defaultValue) {
        String v = getStringAttr(name, attributes);
        return parseFloatValue(v, defaultValue);
    }

    private static Float parseFloatValue(String str, Float defaultValue) {
        if (str == null) {
            return defaultValue;
        } else if (str.endsWith("px")) {
            str = str.substring(0, str.length() - 2);
        } else if (str.endsWith("%")) {
            str = str.substring(0, str.length() - 1);
            return Float.parseFloat(str) / 100;
        }
        // Log.d(TAG, "Float parsing '" + name + "=" + v + "'");
        return Float.parseFloat(str);
    }

    private static class Gradient {
        Matrix matrix = null;
        public Shader shader = null;
        public boolean boundingBox = false;

    }

    private static class StyleSet {
        HashMap<String, String> styleMap = new HashMap<String, String>();

        private StyleSet(String string) {
            String[] styles = string.split(";");
            for (String s : styles) {
                String[] style = s.split(":");
                if (style.length == 2) {
                    styleMap.put(style[0], style[1]);
                }
            }
        }

        public String getStyle(String name) {
            return styleMap.get(name);
        }
    }

    private static class Properties {
        StyleSet styles = null;
        Attributes atts;

        private Properties(Attributes atts) {
            this.atts = atts;
            String styleAttr = getStringAttr("style", atts);
            if (styleAttr != null) {
                styles = new StyleSet(styleAttr);
            }
        }

        public String getAttr(String name) {
            String v = null;
            if (styles != null) {
                v = styles.getStyle(name);
            }
            if (v == null) {
                v = getStringAttr(name, atts);
            }
            return v;
        }

        public String getString(String name) {
            return getAttr(name);
        }

        private Integer rgb(int r, int g, int b) {
            return ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
        }

        private int parseNum(String v) throws NumberFormatException {
            if (v.endsWith("%")) {
                v = v.substring(0, v.length() - 1);
                return Math.round(Float.parseFloat(v) / 100 * 255);
            }
            return Integer.parseInt(v);
        }

        public Integer getColor(String name) {
            String v = name;
            LogHelper.warn("xxxxx", "color name:" + v);
            if (v == null) {
                return null;
            } else if (v.startsWith("#")) {
                try { // #RRGGBB or #AARRGGBB
                    return Color.parseColor(v);
                } catch (IllegalArgumentException iae) {
                    return null;
                }
            } else if (v.startsWith("rgb(") && v.endsWith(")")) {
                String values[] = v.substring(4, v.length() - 1).split(",");
                try {
                    return rgb(parseNum(values[0]), parseNum(values[1]), parseNum(values[2]));
                } catch (NumberFormatException nfe) {
                    return null;
                } catch (ArrayIndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                return Color.TRANSPARENT;
            }
        }

        public Float getFloat(String name, Float defaultValue) {
            String v = getAttr(name);
            if (v == null) {
                return defaultValue;
            } else {
                try {
                    return Float.parseFloat(v);
                } catch (NumberFormatException nfe) {
                    return defaultValue;
                }
            }
        }

        public Float getFloat(String name) {
            return getFloat(name, null);
        }
    }

    static class SVGHandler extends DefaultHandler {

        private List<Path> pathList;
        private List<Integer> colorList;
        private List<Float> strokeWidthList;

        float canvasWidth;
        float canvasHeight;
        float widthScale;
        float heightScale;
        float strokeScale;

        // Scratch rect (so we aren't constantly making new ones)
        final RectF rect = new RectF();

        final HashMap<String, Gradient> gradientMap = new HashMap<String, Gradient>();

        public SVGHandler(float canvasWidth, float canvasHeight) {
            pathList = new ArrayList<>();
            colorList = new ArrayList<>();
            strokeWidthList = new ArrayList<>();
            this.canvasWidth = canvasWidth;
            this.canvasHeight = canvasHeight;
        }

        @Override
        public void startDocument() throws SAXException {
            // Set up prior to parsing a doc
        }

        @Override
        public void endDocument() throws SAXException {
            // Clean up after parsing a doc
        }

        private final Matrix gradMatrix = new Matrix();

        private boolean doFill(Properties atts, RectF bounding_box) {
            if ("none".equals(atts.getString("display"))) {
                return false;
            }
            String fillString = atts.getString("fill");
            if (fillString == null && SVG_FILL != null) {
                fillString = SVG_FILL;
            }
            if (fillString != null) {
                if (fillString.startsWith("url(#")) {

                    // It's a gradient fill, look it up in our map
                    String id = fillString.substring("url(#".length(), fillString.length() - 1);
                    Gradient g = gradientMap.get(id);
                    Shader shader = null;
                    if (g != null) {
                        shader = g.shader;
                    }
                    if (shader != null) {
                        gradMatrix.set(g.matrix);
                        if (g.boundingBox && bounding_box != null) {
                            gradMatrix.preTranslate(bounding_box.left, bounding_box.top);
                            gradMatrix.preScale(bounding_box.width(), bounding_box.height());
                        }
                        shader.setLocalMatrix(gradMatrix);
                        return true;
                    } else {
                        Log.w(TAG, "Didn't find shader, using black: " + id);
                        return true;
                    }
                } else if (fillString.equalsIgnoreCase("none")) {
                    return true;
                } else {
                    Integer color = atts.getColor(fillString);
                    if (color != null) {
                        return true;
                    } else {
                        Log.w(TAG, "Unrecognized fill color, using black: " + fillString);
                        return true;
                    }
                }
            } else {
                return true;
            }
        }

        private boolean doStroke(Properties atts) {
            if ("none".equals(atts.getString("display"))) {
                return false;
            }

            // Check for other stroke attributes
            Float width = atts.getFloat("stroke-width");
            if (width != null) {
                LogHelper.warn("xxxxx", "stroke-width:" + width);
                LogHelper.warn("xxxxx", "width scale:" + widthScale + "\nheight scale:" + heightScale);
                if (strokeScale == 0) strokeScale = 1;
                strokeWidthList.add(width * strokeScale);
            }

            String strokeString = atts.getAttr("stroke");
            if (strokeString != null) {
                if (strokeString.equalsIgnoreCase("none")) {
                    return false;
                } else {
                    Integer color = atts.getColor(strokeString);
                    if (color != null) {
                        colorList.add(color);
                        return true;
                    } else {
                        Log.w(TAG, "Unrecognized stroke color, using none: " + strokeString);
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        private String SVG_FILL = null;

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                throws SAXException {
            if (localName.equals("svg")) {
                SVG_FILL = getStringAttr("fill", atts);
                int width = (int) Math.ceil(getFloatAttr("width", atts));
                int height = (int) Math.ceil(getFloatAttr("height", atts));
                LogHelper.warn("xxxxx", "canvas width:" + width + "  canvas height:" + height);
                if (width != 0 && height != 0) {
                    widthScale = canvasWidth / width;
                    heightScale = canvasHeight / height;
                    strokeScale = widthScale * heightScale;
                }
            } else if (localName.equals("path")) {
                Path p = doPath(getStringAttr("d", atts), widthScale, heightScale);
                pathList.add(p);
                Properties props = new Properties(atts);
                p.computeBounds(rect, false);
                doFill(props, rect);
                doStroke(props);
            } else {
                Log.w(TAG, "UNRECOGNIZED SVG COMMAND: " + localName);
            }
        }
    }
}

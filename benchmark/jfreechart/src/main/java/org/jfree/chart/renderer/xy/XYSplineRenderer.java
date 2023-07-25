/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2021, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates. 
 * Other names may be trademarks of their respective owners.]
 *
 * ---------------------
 * XYSplineRenderer.java
 * ---------------------
 * (C) Copyright 2007-2021, by Klaus Rheinwald and Contributors.
 *
 * Original Author:  Klaus Rheinwald;
 * Contributor(s):   Tobias von Petersdorff (tvp@math.umd.edu,
 *                       http://www.wam.umd.edu/~petersd/);
 *                   David Gilbert (for Object Refinery Limited);
 *
 */

package org.jfree.chart.renderer.xy;

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.GradientPaintTransformer;
import org.jfree.chart.api.RectangleEdge;
import org.jfree.chart.util.StandardGradientPaintTransformer;
import org.jfree.chart.internal.Args;
import org.jfree.data.xy.XYDataset;

/**
 * A renderer that connects data points with natural cubic splines and/or
 * draws shapes at each data point.  This renderer is designed for use with
 * the {@link XYPlot} class. The example shown here is generated by the
 * {@code XYSplineRendererDemo1.java} program included in the JFreeChart
 * demo collection:
 * <br><br>
 * <img src="doc-files/XYSplineRendererSample.png" alt="XYSplineRendererSample.png">
 *
 * @since 1.0.7
 */
public class XYSplineRenderer extends XYLineAndShapeRenderer {

    /**
     * An enumeration of the fill types for the renderer.
     * 
     * @since 1.0.17
     */
    public static enum FillType {
       
        /** No fill. */
        NONE,
        
        /** Fill down to zero. */
        TO_ZERO,
        
        /** Fill to the lower bound. */
        TO_LOWER_BOUND,
        
        /** Fill to the upper bound. */
        TO_UPPER_BOUND
    }
    
    /**
     * Represents state information that applies to a single rendering of
     * a chart.
     */
    public static class XYSplineState extends State {
        
        /** The area to fill under the curve. */
        public GeneralPath fillArea;
        
        /** The points. */
        public List<Point2D> points;
        
        /**
         * Creates a new state instance.
         * 
         * @param info  the plot rendering info. 
         */
        public XYSplineState(PlotRenderingInfo info) {
            super(info);
            this.fillArea = new GeneralPath();
            this.points = new ArrayList<>();
        }
    }
    
    /**
     * Resolution of splines (number of line segments between points)
     */
    private int precision;

    /**
     * A flag that can be set to specify 
     * to fill the area under the spline.
     */
    private FillType fillType;

    private GradientPaintTransformer gradientPaintTransformer;
    
    /**
     * Creates a new instance with the precision attribute defaulting to 5 
     * and no fill of the area 'under' the spline.
     */
    public XYSplineRenderer() {
        this(5, FillType.NONE);
    }

    /**
     * Creates a new renderer with the specified precision 
     * and no fill of the area 'under' (between '0' and) the spline.
     *
     * @param precision  the number of points between data items.
     */
    public XYSplineRenderer(int precision) {
        this(precision, FillType.NONE);
    }

    /**
     * Creates a new renderer with the specified precision
     * and specified fill of the area 'under' (between '0' and) the spline.
     *
     * @param precision  the number of points between data items.
     * @param fillType  the type of fill beneath the curve ({@code null} 
     *     not permitted).
     * 
     * @since 1.0.17
     */
    public XYSplineRenderer(int precision, FillType fillType) {
        super();
        if (precision <= 0) {
            throw new IllegalArgumentException("Requires precision > 0.");
        }
        Args.nullNotPermitted(fillType, "fillType");
        this.precision = precision;
        this.fillType = fillType;
        this.gradientPaintTransformer = new StandardGradientPaintTransformer();
    }

    /**
     * Returns the number of line segments used to approximate the spline
     * curve between data points.
     *
     * @return The number of line segments.
     *
     * @see #setPrecision(int)
     */
    public int getPrecision() {
        return this.precision;
    }

    /**
     * Set the resolution of splines and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @param p  number of line segments between points (must be &gt; 0).
     *
     * @see #getPrecision()
     */
    public void setPrecision(int p) {
        if (p <= 0) {
            throw new IllegalArgumentException("Requires p > 0.");
        }
        this.precision = p;
        fireChangeEvent();
    }

    /**
     * Returns the type of fill that the renderer draws beneath the curve.
     *
     * @return The type of fill (never {@code null}).
     *
     * @see #setFillType(FillType) 
     * 
     * @since 1.0.17
     */
    public FillType getFillType() {
        return this.fillType;
    }

    /**
     * Set the fill type and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @param fillType   the fill type ({@code null} not permitted).
     *
     * @see #getFillType()
     * 
     * @since 1.0.17
     */
    public void setFillType(FillType fillType) {
        this.fillType = fillType;
        fireChangeEvent();
    }

    /**
     * Returns the gradient paint transformer, or {@code null}.
     * 
     * @return The gradient paint transformer (possibly {@code null}).
     * 
     * @since 1.0.17
     */
    public GradientPaintTransformer getGradientPaintTransformer() {
        return this.gradientPaintTransformer;
    }
    
    /**
     * Sets the gradient paint transformer and sends a 
     * {@link RendererChangeEvent} to all registered listeners.
     * 
     * @param gpt  the transformer ({@code null} permitted).
     * 
     * @since 1.0.17
     */
    public void setGradientPaintTransformer(GradientPaintTransformer gpt) {
        this.gradientPaintTransformer = gpt;
        fireChangeEvent();
    }
    
    /**
     * Initialises the renderer.
     * <P>
     * This method will be called before the first item is rendered, giving the
     * renderer an opportunity to initialise any state information it wants to
     * maintain.  The renderer can do nothing if it chooses.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area inside the axes.
     * @param plot  the plot.
     * @param data  the data.
     * @param info  an optional info collection object to return data back to
     *              the caller.
     *
     * @return The renderer state.
     */
    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea,
            XYPlot plot, XYDataset data, PlotRenderingInfo info) {

        setDrawSeriesLineAsPath(true);
        XYSplineState state = new XYSplineState(info);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    /**
     * Draws the item (first pass). This method draws the lines
     * connecting the items. Instead of drawing separate lines,
     * a GeneralPath is constructed and drawn at the end of
     * the series painting.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param plot  the plot (can be used to obtain standard color information
     *              etc).
     * @param dataset  the dataset.
     * @param pass  the pass.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param xAxis  the domain axis.
     * @param yAxis  the range axis.
     * @param dataArea  the area within which the data is being drawn.
     */
    @Override
    protected void drawPrimaryLineAsPath(XYItemRendererState state,
            Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
            int series, int item, ValueAxis xAxis, ValueAxis yAxis,
            Rectangle2D dataArea) {

        XYSplineState s = (XYSplineState) state;
        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

        // get the data points
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);
        double transX1 = xAxis.valueToJava2D(x1, dataArea, xAxisLocation);
        double transY1 = yAxis.valueToJava2D(y1, dataArea, yAxisLocation);

        // Collect points
        if (!Double.isNaN(transX1) && !Double.isNaN(transY1)) {
            Point2D p = plot.getOrientation() == PlotOrientation.HORIZONTAL 
                ? new Point2D.Float((float) transY1, (float) transX1) 
                : new Point2D.Float((float) transX1, (float) transY1);
            if (!s.points.contains(p))
                s.points.add(p);
        }
        
        if (item == dataset.getItemCount(series) - 1) {     // construct path
            if (s.points.size() > 1) {
                Point2D origin;
                if (this.fillType == FillType.TO_ZERO) {
                    float xz = (float) xAxis.valueToJava2D(0, dataArea, 
                            yAxisLocation);
                    float yz = (float) yAxis.valueToJava2D(0, dataArea, 
                            yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(yz, xz) 
                            : new Point2D.Float(xz, yz);
                } else if (this.fillType == FillType.TO_LOWER_BOUND) {
                    float xlb = (float) xAxis.valueToJava2D(
                            xAxis.getLowerBound(), dataArea, xAxisLocation);
                    float ylb = (float) yAxis.valueToJava2D(
                            yAxis.getLowerBound(), dataArea, yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(ylb, xlb) 
                            : new Point2D.Float(xlb, ylb);
                } else {// fillType == TO_UPPER_BOUND
                    float xub = (float) xAxis.valueToJava2D(
                            xAxis.getUpperBound(), dataArea, xAxisLocation);
                    float yub = (float) yAxis.valueToJava2D(
                            yAxis.getUpperBound(), dataArea, yAxisLocation);
                    origin = plot.getOrientation() == PlotOrientation.HORIZONTAL
                            ? new Point2D.Float(yub, xub)
                            : new Point2D.Float(xub, yub);
                }
                
                // we need at least two points to draw something
                Point2D cp0 = s.points.get(0);
                s.seriesPath.moveTo(cp0.getX(), cp0.getY());
                if (this.fillType != FillType.NONE) {
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        s.fillArea.moveTo(origin.getX(), cp0.getY());
                    } else {
                        s.fillArea.moveTo(cp0.getX(), origin.getY());
                    }
                    s.fillArea.lineTo(cp0.getX(), cp0.getY());
                }
                if (s.points.size() == 2) {
                    // we need at least 3 points to spline. Draw simple line
                    // for two points
                    Point2D cp1 = s.points.get(1);
                    if (this.fillType != FillType.NONE) {
                        s.fillArea.lineTo(cp1.getX(), cp1.getY());
                        s.fillArea.lineTo(cp1.getX(), origin.getY());
                        s.fillArea.closePath();
                    }
                    s.seriesPath.lineTo(cp1.getX(), cp1.getY());
                } else {
                    // construct spline
                    int np = s.points.size(); // number of points
                    float[] d = new float[np]; // Newton form coefficients
                    float[] x = new float[np]; // x-coordinates of nodes
                    float y, oldy;
                    float t, oldt;

                    float[] a = new float[np];
                    float t1;
                    float t2;
                    float[] h = new float[np];

                    for (int i = 0; i < np; i++) {
                        Point2D.Float cpi = (Point2D.Float) s.points.get(i);
                        x[i] = cpi.x;
                        d[i] = cpi.y;
                    }

                    for (int i = 1; i <= np - 1; i++)
                        h[i] = x[i] - x[i - 1];

                    float[] sub = new float[np - 1];
                    float[] diag = new float[np - 1];
                    float[] sup = new float[np - 1];

                    for (int i = 1; i <= np - 2; i++) {
                        diag[i] = (h[i] + h[i + 1]) / 3;
                        sup[i] = h[i + 1] / 6;
                        sub[i] = h[i] / 6;
                        a[i] = (d[i + 1] - d[i]) / h[i + 1]
                                   - (d[i] - d[i - 1]) / h[i];
                    }
                    solveTridiag(sub, diag, sup, a, np - 2);

                    // note that a[0]=a[np-1]=0
                    oldt = x[0];
                    oldy = d[0];
                    for (int i = 1; i <= np - 1; i++) {
                        // loop over intervals between nodes
                        for (int j = 1; j <= this.precision; j++) {
                            t1 = (h[i] * j) / this.precision;
                            t2 = h[i] - t1;
                            y = ((-a[i - 1] / 6 * (t2 + h[i]) * t1 + d[i - 1])
                                    * t2 + (-a[i] / 6 * (t1 + h[i]) * t2
                                    + d[i]) * t1) / h[i];
                            t = x[i - 1] + t1;
                            s.seriesPath.lineTo(t, y);
                            if (this.fillType != FillType.NONE) {
                                s.fillArea.lineTo(t, y);
                            }
                        }
                    }
                }
                // Add last point @ y=0 for fillPath and close path
                if (this.fillType != FillType.NONE) {
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        s.fillArea.lineTo(origin.getX(), s.points.get(
                                s.points.size() - 1).getY());
                    } else {
                        s.fillArea.lineTo(s.points.get(
                                s.points.size() - 1).getX(), origin.getY());
                    }
                    s.fillArea.closePath();
                }

                // fill under the curve...
                if (this.fillType != FillType.NONE) {
                    Paint fp = getSeriesFillPaint(series);
                    if (this.gradientPaintTransformer != null 
                            && fp instanceof GradientPaint) {
                        GradientPaint gp = this.gradientPaintTransformer
                                .transform((GradientPaint) fp, s.fillArea);
                        g2.setPaint(gp);
                    } else {
                        g2.setPaint(fp);                        
                    }
                    g2.fill(s.fillArea);
                    s.fillArea.reset();
                }
                // then draw the line...
                drawFirstPassShape(g2, pass, series, item, s.seriesPath);
            }
            // reset points vector
            s.points = new ArrayList<>();
        }
    }
    
    private void solveTridiag(float[] sub, float[] diag, float[] sup,
            float[] b, int n) {
/*      solve linear system with tridiagonal n by n matrix a
        using Gaussian elimination *without* pivoting
        where   a(i,i-1) = sub[i]  for 2<=i<=n
        a(i,i)   = diag[i] for 1<=i<=n
        a(i,i+1) = sup[i]  for 1<=i<=n-1
        (the values sub[1], sup[n] are ignored)
        right hand side vector b[1:n] is overwritten with solution
        NOTE: 1...n is used in all arrays, 0 is unused */
        int i;
/*      factorization and forward substitution */
        for (i = 2; i <= n; i++) {
            sub[i] /= diag[i - 1];
            diag[i] -= sub[i] * sup[i - 1];
            b[i] -= sub[i] * b[i - 1];
        }
        b[n] /= diag[n];
        for (i = n - 1; i >= 1; i--)
            b[i] = (b[i] - sup[i] * b[i + 1]) / diag[i];
    }

    /**
     * Tests this renderer for equality with an arbitrary object.
     *
     * @param obj  the object ({@code null} permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof XYSplineRenderer)) {
            return false;
        }
        XYSplineRenderer that = (XYSplineRenderer) obj;
        if (this.precision != that.precision) {
            return false;
        }
        if (this.fillType != that.fillType) {
            return false;
        }
        if (!Objects.equals(this.gradientPaintTransformer, that.gradientPaintTransformer)) {
            return false;
        }
        return super.equals(obj);
    }
}

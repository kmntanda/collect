package org.odk.collect.geo.support

import androidx.fragment.app.Fragment
import org.odk.collect.maps.MapFragment
import org.odk.collect.maps.MapFragment.FeatureListener
import org.odk.collect.maps.MapFragment.PointListener
import org.odk.collect.maps.MapFragment.ReadyListener
import org.odk.collect.maps.MapPoint
import org.odk.collect.maps.markers.MarkerDescription
import org.odk.collect.maps.markers.MarkerIconDescription
import kotlin.random.Random

class FakeMapFragment : Fragment(), MapFragment {

    private var clickListener: PointListener? = null
    private var gpsLocationListener: PointListener? = null
    private var locationProvider: String? = null
    private var retainMockAccuracy = false
    private var center: MapPoint? = null
    private var zoom = 0.0
    private var zoomBoundingBox: Pair<Iterable<MapPoint>, Double>? = null
    private var readyListener: ReadyListener? = null
    private var gpsLocation: MapPoint? = null
    private var featureClickListener: FeatureListener? = null
    private val markers = mutableMapOf<Int, MapPoint>()
    private val markerIcons = mutableMapOf<Int, MarkerIconDescription?>()
    private val polyLines = mutableMapOf<Int, List<MapPoint>>()
    private val polyClosed: MutableList<Boolean> = ArrayList()
    private val polyDraggable: MutableList<Boolean> = ArrayList()
    private val polygons = mutableMapOf<Int, List<MapPoint>>()
    private var hasCenter = false
    private val featureIds = mutableListOf<Int>()

    override fun init(
        readyListener: ReadyListener?,
        errorListener: MapFragment.ErrorListener?,
    ) {
        this.readyListener = readyListener
    }

    fun ready() {
        readyListener?.onReady(this)
    }

    override fun getCenter(): MapPoint {
        return center ?: DEFAULT_CENTER
    }

    override fun getZoom(): Double {
        return zoom
    }

    override fun setCenter(center: MapPoint?, animate: Boolean) {
        this.center = center
        hasCenter = true
    }

    override fun zoomToPoint(center: MapPoint?, animate: Boolean) {
        zoomBoundingBox = null
        this.center = center
        this.zoom = DEFAULT_POINT_ZOOM
        hasCenter = true
    }

    override fun zoomToPoint(center: MapPoint?, zoom: Double, animate: Boolean) {
        zoomBoundingBox = null
        this.center = center
        this.zoom = zoom
        hasCenter = true
    }

    override fun zoomToBoundingBox(
        points: Iterable<MapPoint>,
        scaleFactor: Double,
        animate: Boolean,
    ) {
        center = null
        zoom = 0.0
        zoomBoundingBox = Pair(
            points.toList(), // Clone list to prevent original changing captured values
            scaleFactor
        )
        hasCenter = true
    }

    override fun addMarker(markerDescription: MarkerDescription): Int {
        val featureId = generateFeatureId()

        markers[featureId] = markerDescription.point
        markerIcons[featureId] = markerDescription.iconDescription

        featureIds.add(featureId)
        return featureId
    }

    override fun addMarkers(markers: List<MarkerDescription>): List<Int> {
        return markers.map {
            addMarker(it)
        }
    }

    override fun setMarkerIcon(featureId: Int, markerIconDescription: MarkerIconDescription) {
        markerIcons[featureId] = markerIconDescription
    }

    override fun getMarkerPoint(featureId: Int): MapPoint {
        return markers[featureId]!!
    }

    override fun addPolyLine(
        points: Iterable<MapPoint>,
        closed: Boolean,
        draggable: Boolean
    ): Int {
        val featureId = generateFeatureId()

        polyLines[featureId] = points.toList()
        polyClosed.add(closed)
        polyDraggable.add(draggable)

        featureIds.add(featureId)
        return featureId
    }

    override fun addPolygon(points: MutableIterable<MapPoint>): Int {
        val featureId = generateFeatureId()
        polygons[featureId] = points.toList()
        featureIds.add(featureId)
        return featureId
    }

    override fun appendPointToPolyLine(featureId: Int, point: MapPoint) {
        val poly = polyLines[featureId]!!
        polyLines[featureId] = poly + point
    }

    override fun removePolyLineLastPoint(featureId: Int) {
        val poly = polyLines[featureId]!!
        polyLines[featureId] = poly.dropLast(1)
    }

    override fun getPolyLinePoints(featureId: Int): List<MapPoint> {
        return polyLines[featureId]!!
    }

    override fun clearFeatures() {
        markers.clear()
        markerIcons.clear()
    }

    override fun setClickListener(listener: PointListener?) {
        this.clickListener = listener
    }

    fun click(point: MapPoint) {
        clickListener?.onPoint(point)
    }

    override fun setLongPressListener(listener: PointListener?) {}
    override fun setFeatureClickListener(listener: FeatureListener?) {
        featureClickListener = listener
    }

    override fun setDragEndListener(listener: FeatureListener?) {}
    override fun setGpsLocationEnabled(enabled: Boolean) {}
    override fun getGpsLocation(): MapPoint? {
        return gpsLocation
    }

    override fun getLocationProvider(): String? {
        return locationProvider
    }

    override fun runOnGpsLocationReady(listener: ReadyListener) {}
    override fun setGpsLocationListener(listener: PointListener?) {
        gpsLocationListener = listener

        gpsLocation?.let {
            listener?.onPoint(it)
        }
    }

    override fun setRetainMockAccuracy(retainMockAccuracy: Boolean) {
        this.retainMockAccuracy = retainMockAccuracy
    }

    override fun hasCenter(): Boolean {
        return hasCenter
    }

    fun setLocation(mapPoint: MapPoint?) {
        gpsLocation = mapPoint
        if (gpsLocationListener != null) {
            gpsLocationListener!!.onPoint(mapPoint!!)
        }
    }

    fun setLocationProvider(locationProvider: String?) {
        this.locationProvider = locationProvider
    }

    fun isRetainMockAccuracy(): Boolean {
        return retainMockAccuracy
    }

    fun clickOnFeature(index: Int) {
        featureClickListener!!.onFeature(featureIds[index])
    }

    fun clickOnFeatureId(featureId: Int) {
        featureClickListener!!.onFeature(featureId)
    }

    fun getMarkers(): List<MapPoint> {
        return markers.values.toList()
    }

    fun getMarkerIcons(): List<MarkerIconDescription?> {
        return markerIcons.values.toList()
    }

    fun getZoomBoundingBox(): Pair<Iterable<MapPoint>, Double>? {
        return zoomBoundingBox
    }

    fun getPolyLines(): List<List<MapPoint>> {
        return polyLines.values.toList()
    }

    fun isPolyClosed(index: Int): Boolean {
        return polyClosed[index]
    }

    fun isPolyDraggable(index: Int): Boolean {
        return polyDraggable[index]
    }

    fun getFeatureId(points: List<MapPoint>): Int {
        return if (points.size == 1) {
            markers.entries.find {
                it.value == points[0]
            }!!.key
        } else {
            polyLines.entries.find {
                it.value == points
            }!!.key
        }
    }

    private fun generateFeatureId(): Int {
        var featureId = Random.nextInt()
        while (featureIds.contains(featureId)) {
            featureId = Random.nextInt()
        }

        return featureId
    }

    fun getPolygons(): List<List<MapPoint>> {
        return polygons.values.toList()
    }

    companion object {
        /**
         * The value returned if the map has had no center set or has had `null` pass to
         * [setCenter]
         */
        val DEFAULT_CENTER = MapPoint(-1.0, -1.0)

        /**
         * The value used to zoom when [zoomToPoint] is called without a zoom level
         */
        const val DEFAULT_POINT_ZOOM = -1.0
    }
}

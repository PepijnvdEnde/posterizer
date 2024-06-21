package nl.pep.imageview

import javafx.animation.PauseTransition
import javafx.beans.property.ReadOnlyProperty
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.util.Duration
import java.io.File
import java.awt.Color as AwtColor

class LoadImgController {

    @FXML
    lateinit var imageView: ImageView

    @FXML
    lateinit var stepsLabel: Label

    @FXML
    lateinit var brightnessLabel: Label


    @FXML
    lateinit var tintLabel: Label

    @FXML
    lateinit var saturationLabel: Label


    @FXML
    lateinit var brightnessSlider: Slider

    @FXML
    private lateinit var stepsSpinner: Spinner<Int>

    @FXML
    lateinit var tintSlider: Slider

    @FXML
    lateinit var saturationSlider: Slider

    @FXML
    lateinit var baseColorPicker: ColorPicker

    @FXML
    lateinit var useBaseColorCheckBox: CheckBox

    private var steps: Int = 250

    private var originalImage: Image? = null

    fun initialize() {
        val delay = PauseTransition(Duration.seconds(0.2))
        delay.setOnFinished { _ -> updateImage() }

        setupInputFields(stepsSpinner.valueProperty(), stepsLabel, "Steps", delay)
        stepsSpinner.valueProperty().addListener { _, _, newValue -> steps = newValue }
        setupInputFields(brightnessSlider.valueProperty(), brightnessLabel, "Brightness", delay)
        setupInputFields(tintSlider.valueProperty(), tintLabel, "Tint", delay)
        setupInputFields(saturationSlider.valueProperty(), saturationLabel, "Saturation", delay)
        setupInputFields(baseColorPicker.valueProperty(), delay = delay)
        setupInputFields(useBaseColorCheckBox.selectedProperty(), delay = delay)
    }

    private fun <T> setupInputFields(property: ReadOnlyProperty<T>, label: Label? = null, labelPrefix: String = "", delay: PauseTransition) {
        label?.text = "$labelPrefix: ${property.value}"
        property.addListener { _, _, newValue ->
            label?.text = "$labelPrefix: $newValue"
            delay.playFromStart()
        }
    }

    @Suppress("UNUSED")
    @FXML
    private fun onLoadImageButtonClick() {
        val fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"))
        fileChooser.initialDirectory = File(System.getProperty("user.home") + "\\Pictures")
        val selectedFile = fileChooser.showOpenDialog(null)
        if (selectedFile != null) {
            originalImage = Image(selectedFile.toURI().toString())
            imageView.image = originalImage
            updateImage()
        }
    }

    private fun updateImage() {
        val image = originalImage
        if (image != null) {
            val pixelReader = image.pixelReader
            val width = image.width.toInt()
            val height = image.height.toInt()
            val writableImage = WritableImage(width, height)
            val pixelWriter = writableImage.pixelWriter

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val color = pixelReader.getColor(x, y)

                    // Convert RGB to HSV
                    val hsbVals = floatArrayOf(0f, 0f, 0f)
                    AwtColor.RGBtoHSB((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt(), hsbVals)

                    val base = getBaseColor()
                    val clampedBri = calculateBrightness(hsbVals[2])
                    val (hue, clampedSat) = calculateHueAndSaturation(hsbVals, base, clampedBri)

                    val newColor = posterizeColor(hue, clampedSat, clampedBri)
                    pixelWriter.setColor(x, y, newColor)
                }
            }
            imageView.image = writableImage
        }
    }

    private fun getBaseColor(): Color? {
        return if (useBaseColorCheckBox.isSelected) baseColorPicker.value else null
    }

    private fun calculateBrightness(brightness: Float): Double {
        val sliderValue = brightnessSlider.value
        val bri = when {
            sliderValue < 1 -> sliderValue * brightness.toDouble()
            sliderValue == 1.0 -> brightness.toDouble()
            else -> brightness.toDouble() * sliderValue
        }
        return 0.0.coerceAtLeast(bri.coerceAtMost(1.0))
    }

    private fun calculateHueAndSaturation(hsbVals: FloatArray, base: Color?, clampedBri: Double): Pair<Float, Double> {
        val hue = if (base != null) {
            val baseHsbVals = floatArrayOf(0f, 0f, 0f)
            AwtColor.RGBtoHSB((base.red * 255).toInt(), (base.green * 255).toInt(), (base.blue * 255).toInt(), baseHsbVals)
            baseHsbVals[0] + ((baseHsbVals[2] - clampedBri) * tintSlider.value)
        } else {
            hsbVals[0] + tintSlider.value
        }

        // Add the saturation delta to the original saturation, clamp to the range 0.0 - 1.0
        val clampedSat = 0.0.coerceAtLeast((hsbVals[1] + saturationSlider.value).coerceAtMost(1.0))

        return Pair(hue.toFloat(), clampedSat)
    }

    private fun posterizeColor(hue: Float, clampedSat: Double, clampedBri: Double): Color {
        // Posterize the color by rounding the product of the color and steps, then dividing by steps
        val posterizedHue = (Math.round(hue * steps) / steps.toDouble()).toFloat()
        val posterizedSat = (Math.round(clampedSat * steps) / steps.toDouble()).toFloat()
        val posterizedBri = (Math.round(clampedBri * steps) / steps.toDouble()).toFloat()

        return Color.hsb(posterizedHue.toDouble() * 360, posterizedSat.toDouble(), posterizedBri.toDouble())
    }
}

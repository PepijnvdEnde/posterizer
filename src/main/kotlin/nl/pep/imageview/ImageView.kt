package nl.pep.imageview

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class ImageView : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(ImageView::class.java.getResource("Image-view.fxml"))
        val scene = Scene(fxmlLoader.load(), 1920.0, 1000.0)
        stage.title = "ImageView"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(ImageView::class.java)
}
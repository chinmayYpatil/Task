package com.example.task.platform

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import platform.darwin.NSObject

actual class CameraLauncher : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    private val _capturedImagePath = MutableSharedFlow<String>(extraBufferCapacity = 1)
    actual val capturedImagePath: SharedFlow<String> = _capturedImagePath

    actual fun launch() {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController

        if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            val picker = UIImagePickerController()
            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            picker.delegate = this
            rootViewController?.presentViewController(picker, animated = true, completion = null)
        }
    }

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image != null) {
            val data = UIImageJPEGRepresentation(image, 0.8)
            val path = NSTemporaryDirectory() + "photo_${NSDate().timeIntervalSince1970}.jpg"
            data?.writeToFile(path, true)
            _capturedImagePath.tryEmit(path)
        }
        picker.dismissViewControllerAnimated(true, completion = null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

actual fun getCameraLauncher(): CameraLauncher = CameraLauncher()
package com.dicoding.picodiploma.mycamera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.picodiploma.mycamera.databinding.ActivityResultBinding
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var detectedResultText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            binding.resultImage.setImageURI(imageUri)
            analyzeBarcodeFromUri(imageUri)
        } else {
            binding.resultText.text = getString(R.string.no_text_recognized)
        }

        binding.copyButton.setOnClickListener {
            if (detectedResultText.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Barcode Text", detectedResultText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Teks barcode berhasil disalin!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.openLinkButton.setOnClickListener {
            if (detectedResultText.startsWith("http://") || detectedResultText.startsWith("https://")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detectedResultText))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Hasil barcode bukan URL link valid", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun analyzeBarcodeFromUri(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(this, uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val firstBarcode = barcodes.first()
                        detectedResultText = firstBarcode.rawValue ?: ""
                        binding.resultText.text = detectedResultText.ifEmpty { getString(R.string.no_text_recognized) }

                        if (detectedResultText.startsWith("http://") || detectedResultText.startsWith("https://")) {
                            binding.openLinkButton.visibility = View.VISIBLE
                        } else {
                            binding.openLinkButton.visibility = View.GONE
                        }
                    } else {
                        binding.resultText.text = getString(R.string.no_text_recognized)
                        binding.openLinkButton.visibility = View.GONE
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ResultActivity", "Analysis error", e)
                    binding.resultText.text = "Gagal menganalisis gambar: ${e.message}"
                }
        } catch (e: Exception) {
            Log.e("ResultActivity", "Error loading image", e)
            binding.resultText.text = "Gagal memuat gambar: ${e.message}"
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT = "extra_result"
    }
}
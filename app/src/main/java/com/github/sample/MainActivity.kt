package com.github.sample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kamsyview.extensions.loadAvatar
import com.github.sample.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var sampleAdapter: SampleAdapter

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emptyUrl = ""
        val image = "https://images.pexels.com/photos/14653174/pexels-photo-14653174.jpeg"
        val gifUrl = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExdnFqdGp4cGtkeTl4YWk3ZHg2Z252bjVsMDJlcjE3ZmR3a3F3cXFndCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/ZXkraFrlIW1D25M6ZJ/giphy.gif"
        val svgUrl = "https://res.cloudinary.com/dsm4ilm0j/image/upload/v1752113333/6769264_60111_mrgb8u.svg"

        /*binding.avatar.loadAvatar(
            image,
            "Mbuodile Obiosio",
            blurHash = "LKO2:N%2Tw=w]~RBVZRi};RPxuwH"
        )*/

        setupWindowInsets()

        initAdapter()
    }

    private fun initAdapter() = with(binding) {
        sampleAdapter = SampleAdapter()
        //list.adapter = sampleAdapter
        sampleAdapter.submitList(getUsers())
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).let { systemBars ->
                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                )
            }
            insets
        }
    }
}

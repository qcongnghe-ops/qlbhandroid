package vn.qcongnghe.qlbanhang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.qcongnghe.qlbanhang.data.AuthRepository
import vn.qcongnghe.qlbanhang.data.PosDatabase
import vn.qcongnghe.qlbanhang.data.PosRepository
import vn.qcongnghe.qlbanhang.ui.PosApp
import vn.qcongnghe.qlbanhang.ui.PosViewModel
import vn.qcongnghe.qlbanhang.ui.PosViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = PosRepository(PosDatabase.getInstance(applicationContext))
        val auth = AuthRepository(repository)
        setContent {
            val vm: PosViewModel = viewModel(factory = PosViewModelFactory(repository, auth))
            PosApp(viewModel = vm)
        }
    }
}

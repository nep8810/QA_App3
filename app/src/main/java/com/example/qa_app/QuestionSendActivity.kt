package com.example.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_send.*

import java.io.ByteArrayOutputStream
import java.util.HashMap

class QuestionSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    private var mGenre: Int = 0
    private var mPictureUri: Uri? = null

    // - - - ↓ 渡ってきたジャンルの保持＆UIの準備 ↓ - - -
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // Intentで渡ってきたgenre(ジャンル)の番号を取り出してmGenreで保持する
        val extras = intent.extras
        mGenre = extras.getInt("genre")

        // UIの準備(タイトル設定)
        title = "質問作成"

        sendButton.setOnClickListener(this)
        imageView.setOnClickListener(this)
    }
    // - - - ↑ 渡ってきたジャンルの保持＆UIの準備 ↑ - - -

    // - - - ↓ onActivityResultメソッドでIntent連携から戻ってきた時に画像を取得しリサイズしてImageViewに設定 ↓ - - -
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CHOOSER_REQUEST_CODE) {

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            // 画像を取得
            // dataがnullかdata.getData()の場合はカメラで撮影したときなので画像の取得にmPictureUriを使う
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URIからBitmapを取得する
            val image: Bitmap
            try {
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e: Exception) {
                return
            }

            // 取得したBimapの長辺を500ピクセルにリサイズする
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight) // (1)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizedImage = Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

            // BitmapをImageViewに設定する
            imageView.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }
    // - - - ↑ onActivityResultメソッドでIntent連携から戻ってきた時に画像を取得しリサイズしてImageViewに設定 ↑ - - -

    // - - - ↓ ImageViewとButtonがタップされた時の処理 ↓ - - -
    override fun onClick(v: View) {

        if (v === imageView) {  // 参照等価(同じオブジェクトを指している)
            // パーミッションの許可状態を確認する
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android6.0以降であればcheckSelfPermissionメソッドで外部ストレージへの書き込みが許可
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // showChooserメソッドを呼び出し、Intent連携でギャラリーとカメラを選択するダイアログを表示させる
                    showChooser()
                } else {
                    // 許可されていないので許可ダイアログを表示する
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)

                    return
                }
            } else {
                // Android5以前の場合はパーミッションの許可状態を確認せずにshowChooserメソッドを呼び出す
                showChooser()
            }
        } else if (v === sendButton) {  // 参照等価(同じオブジェクトを指している)
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            val dataBaseReference = FirebaseDatabase.getInstance().reference
            // 参照するものをdataBaseReference.child()内に記述
            val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())

            val data = HashMap<String, String>()

            // UID
            data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

            // タイトルと本文を取得する
            val title = titleText.text.toString()
            val body = bodyText.text.toString()

            if (title.isEmpty()) {
                // タイトルが入力されていない時はエラーを表示するだけ
                Snackbar.make(v, "タイトルを入力して下さい", Snackbar.LENGTH_LONG).show()
                return
            }

            if (body.isEmpty()) {
                // 質問が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, "質問を入力して下さい", Snackbar.LENGTH_LONG).show()
                return
            }

            // Preferenceから名前を取得する
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val name = sp.getString(NameKEY, "")

            data["title"] = title
            data["body"] = body
            data["name"] = name

            // 添付画像を取得する
            // as?は安全なキャスト演算子(キャストに失敗した場合にnullを返す)、画像が設定されていない状態でキャストするとアプリが落ちるため、nullを返すようにしている
            val drawable = imageView.drawable as? BitmapDrawable


            if (drawable != null) {
                val bitmap = drawable.bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)

                // 添付画像が設定されていれば画像を取り出してBASE64エンコード(データを文字列に変換)する
                // Firebaseは文字列や数字しか保存できないがエンコードすることで画像をFirebaseに保存することが可能となる
                val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                data["image"] = bitmapString
            }

            genreRef.push().setValue(data, this)
            progressBar.visibility = View.VISIBLE
        }
    }
    // - - - ↑ ImageViewとButtonがタップされた時の処理 ↑ - - -

    // - - - ↓ onRequestPermissionsResultメソッドは許可ダイアログでユーザが選択した結果を受け取る ↓ - - -
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {

                // if (grantResults[0] == PackageManager.PERMISSION_GRANTED)：ユーザーが許可したかどうかを判断できる
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したときshowChooserメソッドを呼び出す
                    showChooser()
                }
                return
            }
        }
    }
    // - - - ↑ onRequestPermissionsResultメソッドは許可ダイアログでユーザが選択した結果を受け取る ↑ - - -

    // - - - ↓ showChooserメソッドはIntent連携でギャラリーとカメラを選択するダイアログを表示する ↓ - - -
    private fun showChooser() {
        // ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        val chooserIntent = Intent.createChooser(galleryIntent, "画像を取得")

        // EXTRA_INITIAL_INTENTS にカメラ撮影のIntentを追加
        // 第一引数にギャラリーから選択するIntent、第二引数にカメラで撮影するIntentを設定しchooserIntentとすることで、2つのIntentを選択するダイアログを表示させることできる
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }
    // - - - ↑ showChooserメソッドはIntent連携でギャラリーとカメラを選択するダイアログを表示する ↑ - - -

    // - - - ↓ onCompleteはFirebaseへの保存完了時に呼ばれる ↓ - - -
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            // Firebaseへの保存が完了したらfinishメソッドを呼び出してActivityを閉じる
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show()
        }
    }
    // - - - ↑ onCompleteはFirebaseへの保存完了時に呼ばれる ↑ - - -
}

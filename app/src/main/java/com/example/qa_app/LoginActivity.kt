package com.example.qa_app

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_login.*

import java.util.HashMap

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private lateinit var mDataBaseReference: DatabaseReference

    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // データベースへのリファレンスを取得
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // FirebaseAuthのオブジェクトを取得する
        mAuth = FirebaseAuth.getInstance()

        // アカウント作成処理のリスナー
        // Firebaseのアカウント作成処理はOnCompleteListenerで受け取る
        mCreateAccountListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                // ログインを行う
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)
            } else {

                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "アカウント作成に失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // ログイン処理のリスナー(OnCompleteListener:処理の完了を受け取るリスナー)
        // Firebaseのログイン処理もOnCompleteListenerクラスで受け取る
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                val user = mAuth.currentUser
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid) //uid=ユーザID

                // アカウント新規作成の時は表示名をFirebaseに保存する
                // Firebaseは、データをKeyとValueの組み合わせで保存する
                if (mIsCreateAccount) {

                    val name = nameText.text.toString()

                    val data = HashMap<String, String>()
                    data["name"] = name
                    // setValueメソッドでDatabaseReferenceが指し示すKeyにValueを保存する
                    userRef.setValue(data)

                    // Firebaseから表示名を取得してPreferenceに保存
                    saveName(name)
                    Log.d("ログイン","完了")  // 《保存の確認》

                } else {
                    // アカウントが既に存在している場合　→　データベースからの読み込み
                    // Firebaseからデータを一度だけ読み取りする場合にはDatabaseReferenceクラスが実装しているQueryクラスのaddListenerForSingleValueEventメソッドを使う
                    // ValueEventListenerで変更を検知
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        // onDataChange()を使用して、特定のパスにあるコンテンツのスナップショット(ある時点でのストレージ上のファイルシステムのコピー)を、イベントの発生時に存在していたとおりに読み取ることができる
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?
                            saveName(data!!["name"] as String)
                        }
                        //読み込み失敗(中止された時の処理はonCancelledメソッドを使う)
                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE

                // Activityを閉じる
                finish()

            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "ログインに失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // UIの準備
        title = "ログイン"

        // - - - ↓ アカウント作成ボタンのOnClickListenerを設定 ↓ - - -
        createButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                // ログイン時に表示名を保存するようにフラグを立てる(変数に「on」に相当する値を入れる)
                mIsCreateAccount = true
                Log.d("新規アカウント保存","完了")  // 《保存の確認》

                // createAccountメソッドを呼び出してアカウント作成処理を開始
                createAccount(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }
        // - - - ↑ アカウント作成ボタンのOnClickListenerを設定 ↑ - - -

        // - - - ↓ ログインボタンのOnClickListenerを設定 ↓ - - -
        loginButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // フラグを落としておく
                mIsCreateAccount = false

                login(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }
        // - - - ↑ ログインボタンのOnClickListenerを設定 ↑ - - -
    }

    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // createUserWithEmailAndPasswordメソッドでアカウント作成
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }

    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // ログインする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    // saveNameメソッドは引数で受け取った表示名をPreferenceに保存する
    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        // 保存には Editorクラスを用い、edit()でオブジェクト取得、保存する値がint型の場合、putInt()でキーと値を保存、commit()で実際にファイルに書き込む
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        Log.d("保存","完了")  // 《保存の確認》

        //忘れずにcommitメソッドを呼び出して保存処理を反映させる
        editor.commit()
    }
}
package com.example.qa_app

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_answer_send.*

import java.util.HashMap

class AnswerSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {

    // プロパティとしてIntentで渡ってきたQuestionを保持する変数を定義
    private lateinit var mQuestion: Question

    // onCreateメソッドで渡ってきたQuestionのインスタンスを保持＆UIの準備を行う
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_send)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        // UIの準備
        sendButton.setOnClickListener(this)
    }

    // onCompleteメソッドでFirebaseへの書き込み完了を受け取る
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        // 成功
        if (databaseError == null) {
            finish()
            // 失敗
        } else {
            Snackbar.make(findViewById(android.R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show()
        }

    }

    // 投稿ボタンが押された時の処理
    override fun onClick(v: View) {
        // キーボードが出てたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        // Firebaseを参照(データベースに対してデータを読み書きするにはDatabaseReference のインスタンスが必要)
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        // 参照するものをdataBaseReference.child()内に記述
        val answerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        // HashMapクラスはkeyとvalueの組み合わせで要素を保管する
        val data = HashMap<String, String>()

        // UIDを取得する
        data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

        // 表示名
        // Preferenceから名前を取得する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        data["name"] = name

        // 回答を取得する
        val answer = answerEditText.text.toString()

        if (answer.isEmpty()) {
            // 回答が入力されていない時はエラーを表示するだけ
            Snackbar.make(v, "回答を入力して下さい", Snackbar.LENGTH_LONG).show()
            return
        }
        data["body"] = answer

        progressBar.visibility = View.VISIBLE
        // Firebaseに書き込み
        answerRef.push().setValue(data, this)
    }

}

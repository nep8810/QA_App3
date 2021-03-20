package com.example.qa_app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ListView
import com.google.firebase.FirebaseApp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_send.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>

    // お気に入りボタンタップ時にフラグを立て、Firebaseに保存する
    private  var mIsFavorite = false

    // データに追加・変化があった時に受け取るChildEventListenerを作成
    // onChildAddedメソッドは、要素が追加された時(今回は回答が追加された時)に呼ばれる
    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""
            val questionUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid, questionUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        // 要素に変化があった時(今回は質問に対して回答が投稿された時)に呼ばれるメソッド
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }
        // 要素が削除された時に呼ばれるメソッド
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }
        // 場所の優先度が変更された時に呼ばれるメソッド
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }
        // エラーが起きて(サーバーで失敗orセキュリティルール/Firebaseルールの結果として)削除された時に呼ばれる
        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)


        // - - - ↓ ログイン時にお気に入りボタンを表示 ↓- - -
        // findViewById()→setContentView()の順にしないとViewが用意されていないのにfindViewすることになるのでnullが返る
        val FB = this.findViewById<Button>(R.id.favorite_button)
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // ログインしていなければ「お気に入りボタン」は表示しない(A.setVisibility(View.INVISIBLE)でAを非表示)
            FB.setVisibility(View.INVISIBLE)
        }
        // - - - ↑ ログイン時にお気に入りボタンを表示 ↑- - -

        // - - - ↓ お気に入りボタンのOnClickListenerを設定 ↓ - - -
        favorite_button.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // Firebaseを参照
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            // 参照するものをdataBaseReference.child()内に記述
            val favoriteRef = dataBaseReference.child(UsersPATH).child(FavoritesPATH).child(user!!.uid).child(mQuestion.questionUid)
            // HashMapクラスはkeyとvalueの組み合わせで要素を保管する
            val data = HashMap<String, Any>()

            // UIDを取得する
            data["uid"] = mQuestion.questionUid
            data["genre"] = mQuestion.genre

            val questionUid = dataBaseReference.key ?: ""

            val FB = this.findViewById<Button>(R.id.favorite_button)

            favoriteRef.addValueEventListener(object : ValueEventListener {
                // onDataChange()を使用して、特定のパスにあるコンテンツのスナップショット(ある時点でのストレージ上のファイルシステムのコピー)を、イベントの発生時に存在していたとおりに読み取ることができる
                override fun onDataChange(snapshot: DataSnapshot) {
                }
                //読み込み失敗(中止された時の処理はonCancelledメソッドを使う)
                override fun onCancelled(firebaseError: DatabaseError) {}
            })

            // お気に入り登録済の時は削除する
            if (mIsFavorite) {
                // フラグを落とす
                mIsFavorite = false

                // removeValue()で削除
                favoriteRef.removeValue()
                Snackbar.make(v, "お気に入りから削除しました", Snackbar.LENGTH_LONG).show()
                FB.setText(R.string.label1)

            } else {
                // フラグを立てる(変数に「on」に相当する値を入れる)
                mIsFavorite = true

                // Firebaseに書き込み(push():ノード内に投稿を作成、setValue():完了コールバック )
                favoriteRef.setValue(data,questionUid)
                Snackbar.make(v, "お気に入りに登録しました", Snackbar.LENGTH_LONG).show()
                FB.setText(R.string.label2)
            }
        }
        // - - - ↑ お気に入りボタンのOnClickListenerを設定 ↑ - - -
    }

    // - - - ↓ 他のアクティビティから戻ってきたときの処理 ↓ - - -
    override fun onResume() {
        super.onResume()
        val FB = this.findViewById<Button>(R.id.favorite_button)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // ログインしていなければ「お気に入りボタン」は表示しない
        } else {
            // ログインしていれば「お気に入りボタン」を表示する
            FB.setVisibility(View.VISIBLE)


            // Firebaseを参照
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            // 参照するものをdataBaseReference.child()内に記述
            val favoriteRef =
                dataBaseReference.child(UsersPATH).child(FavoritesPATH).child(user.uid).child(mQuestion.questionUid )

            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val map = dataSnapshot.value as? Map<Any, Any>
                    val questionUid = map?.keys
                    val uid = map?.get("uid") ?: ""

                        Log.i("パターン1", "dataSnapshot.key = " + dataSnapshot.key)
                        Log.i("パターン2", "dataSnapshot.value = " + dataSnapshot.value)
                        Log.i("パターン3", "dataSnapshot = " + dataSnapshot)
                        Log.i("パターン4", "mQuestion.questionUid = " + mQuestion.questionUid)
                        Log.i("パターン5", "questionUid = " + questionUid)
                        Log.i("パターン6", "uid = " + uid)
                        Log.i("パターン7", "dataBaseReference.key = " + dataBaseReference.key)

                        if (dataSnapshot.value == null) {
                            FB.setText(R.string.label1)
                            Log.d("お気に入りif側", "実行完了")  // 《イベントリスナーの確認》

                        } else {
                            FB.setText(R.string.label2)
                            Log.d("お気に入りelse側", "実行完了")  // 《イベントリスナーの確認》
                        }
                }

                //読み込み失敗(中止された時の処理はonCancelledメソッドを使う)
                override fun onCancelled(firebaseError: DatabaseError) {}
            })
        }

    }

}





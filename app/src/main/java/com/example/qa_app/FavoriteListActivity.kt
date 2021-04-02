package com.example.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoriteListActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

            private lateinit var mFToolbar: Toolbar
            private var mGenre = 0

            // - - - ↓ お気に入り一覧画面に付随した記述 ↓- - -
            private lateinit var mDatabaseReference: DatabaseReference
            private lateinit var mListView: ListView
            private lateinit var mQuestion: Question
            private lateinit var mQuestionArrayList: ArrayList<Question>
            private lateinit var mFAdapter: QuestionsListAdapter


            private var mGenreRef: DatabaseReference? = null

            // データに追加・変化があった時に受け取るChildEventListenerを作成
            /* イベントリスナー：取得したリストの質問IDとジャンルから質問データそのものを取得
               mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())に対してのリスナー */
            private val mEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val map = dataSnapshot.value as Map<String, String>
                    val title = map["title"] ?: ""
                    val body = map["body"] ?: ""
                    val name = map["name"] ?: ""
                    val uid = map["uid"] ?: ""
                    val imageString = map["image"] ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }

                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""

                            val answer = Answer(answerBody, answerName, answerUid, key)
                            answerArrayList.add(answer)
                        }
                    }

                    val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                        mGenre, bytes, answerArrayList)
                    mQuestionArrayList.add(question)
                    // notifyDataSetChanged()はデータセットが変更されたことを、登録されているすべてのobserverに通知する
                    mFAdapter.notifyDataSetChanged()
                    Log.d("mEventListener","実行完了")  // 《イベントリスナーの確認》
                }

                // onChildChangedメソッドで要素に変化があった場合の設定(今回は質問に対して回答が投稿された時)
                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    val map = dataSnapshot.value as Map<String, String>

                    // 変更があったQuestionを探す
                    for (question in mQuestionArrayList) {
                        if (dataSnapshot.key.equals(question.questionUid)) {
                            // このアプリで変更がある可能性があるのは回答(Answer)のみ
                            question.answers.clear()
                            val answerMap = map["answers"] as Map<String, String>?
                            if (answerMap != null) {
                                for (key in answerMap.keys) {
                                    val temp = answerMap[key] as Map<String, String>
                                    val answerBody = temp["body"] ?: ""
                                    val answerName = temp["name"] ?: ""
                                    val answerUid = temp["uid"] ?: ""

                                    val answer = Answer(answerBody, answerName, answerUid, key)
                                    question.answers.add(answer)
                                }
                            }

                            mFAdapter.notifyDataSetChanged()
                        }
                    }
                }
                // 要素が削除された時(今回はお気に入りが削除された時)に呼ばれる
                override fun onChildRemoved(p0: DataSnapshot) {

                }

                override fun onChildMoved(p0: DataSnapshot, p1: String?) {

                }

                override fun onCancelled(p0: DatabaseError) {

                }
            }
            // - - - ↑ お気に入り一覧画面に付随した記述 ↑- - -


            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_favorite)
                mFToolbar = findViewById(R.id.toolbar)
                setSupportActionBar(mFToolbar)

                // ナビゲーションドロワーの設定
                val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
                val toggle =
                    ActionBarDrawerToggle(this, drawer, mFToolbar, R.string.app_name, R.string.app_name)
                drawer.addDrawerListener(toggle)
                toggle.syncState()

                val navigationView = findViewById<NavigationView>(R.id.nav_view)
                navigationView.setNavigationItemSelectedListener(this)

                // - - - ↓ お気に入り一覧画面に付随した記述 ↓- - -
                // タイトルの設定
                title = "お気に入り一覧"

                // ListViewの準備
                mListView = findViewById(R.id.listView)
                mFAdapter = QuestionsListAdapter(this)
                mQuestionArrayList = ArrayList<Question>()

                mFAdapter.setQuestionArrayList(mQuestionArrayList)
                mListView.adapter = mFAdapter
                mFAdapter.notifyDataSetChanged()
                Log.i("Favorite,onCreate", "mQuestionArrayList = " + mQuestionArrayList)
                // - - - ↑ お気に入り一覧画面に付随した記述 ↑- - -


                // - - - ↓ お気に入り一覧画面でリストをタップした時に質問詳細画面に遷移 ↓ - - -
                // ListViewのsetOnItemClickListenerメソッドでリスナーを登録
                mListView.setOnItemClickListener { parent, view, position, id ->
                    // Questionのインスタンスを渡して質問詳細画面を起動する
                    val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
                    intent.putExtra("question", mQuestionArrayList[position])
                    startActivity(intent)
                }
                // - - - ↑ お気に入り一覧画面でリストをタップした時に質問詳細画面に遷移 ↑ - - -
            }

    // - - - ↓ 他のアクティビティから戻ってきたときの処理 ↓ - - -
    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            navigationView.menu.findItem(R.id.nav_favorite).setVisible(false)

        }else {
            mQuestionArrayList.clear()

            mDatabaseReference =
                FirebaseDatabase.getInstance().reference     // DatabaseReferenceを組み立てる
            val favoriteRef =
                mDatabaseReference.child(UsersPATH).child(FavoritesPATH).child(user!!.uid)

            // イベントリスナー：お気に入りリストの取得
            favoriteRef.addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val map = dataSnapshot.value as Map<String, Any>
                    val genre = map["genre"] ?: ""
                    val questionUid = map["uid"] ?: ""

                    val QuestionDetailRef =
                        mDatabaseReference.child(ContentsPATH).child(genre.toString())
                            .child(dataSnapshot.key ?: "")
                    QuestionDetailRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val map = dataSnapshot.value as Map<String, String>
                            val title = map["title"] ?: ""
                            val body = map["body"] ?: ""
                            val name = map["name"] ?: ""
                            val uid = map["uid"] ?: ""
                            val imageString = map["image"] ?: ""
                            val bytes =
                                if (imageString.isNotEmpty()) {
                                    Base64.decode(imageString, Base64.DEFAULT)
                                } else {
                                    byteArrayOf()
                                }

                            val answerArrayList = ArrayList<Answer>()
                            val answerMap = map["answers"] as Map<String, String>?
                            if (answerMap != null) {
                                for (key in answerMap.keys) {
                                    val temp = answerMap[key] as Map<String, String>
                                    val answerBody = temp["body"] ?: ""
                                    val answerName = temp["name"] ?: ""
                                    val answerUid = temp["uid"] ?: ""

                                    val answer =
                                        Answer(answerBody, answerName, answerUid, key)
                                    answerArrayList.add(answer)
                                }
                            }

                            val question = Question(
                                title, body, name, uid, questionUid.toString(),
                                genre, bytes, answerArrayList
                            )
                            mQuestionArrayList.add(question)
                            // notifyDataSetChanged()はデータセットが変更されたことを、登録されているすべてのobserverに通知する
                            mFAdapter.notifyDataSetChanged()
                            Log.i(
                                "●Favorite,onResume",
                                "mQuestionArrayList = " + mQuestionArrayList
                            )

                            for (question in mQuestionArrayList) {
                                if (dataSnapshot.key.equals(question.questionUid)) {
                                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                                    question.answers.clear()
                                    val answerMap = map["answers"] as Map<String, String>?
                                    if (answerMap != null) {
                                        for (key in answerMap.keys) {
                                            val temp = answerMap[key] as Map<String, String>
                                            val answerBody = temp["body"] ?: ""
                                            val answerName = temp["name"] ?: ""
                                            val answerUid = temp["uid"] ?: ""

                                            val answer =
                                                Answer(answerBody, answerName, answerUid, key)
                                            question.answers.add(answer)
                                        }
                                    }

                                    mFAdapter.notifyDataSetChanged()
                                }
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {

                        }
                    })

                }

                // onChildChangedメソッドで要素に変化があった場合の設定(今回は質問に対して回答が投稿された時)
                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    val map = dataSnapshot.value as Map<String, String>

                    // 変更があったQuestionを探す
                    for (question in mQuestionArrayList) {
                        if (dataSnapshot.key.equals(question.questionUid)) {
                            // このアプリで変更がある可能性があるのは回答(Answer)のみ
                            question.answers.clear()
                            val answerMap = map["answers"] as Map<String, String>?
                            if (answerMap != null) {
                                for (key in answerMap.keys) {
                                    val temp = answerMap[key] as Map<String, String>
                                    val answerBody = temp["body"] ?: ""
                                    val answerName = temp["name"] ?: ""
                                    val answerUid = temp["uid"] ?: ""

                                    val answer = Answer(answerBody, answerName, answerUid, key)
                                    question.answers.add(answer)
                                }
                            }

                            mFAdapter.notifyDataSetChanged()
                        }
                    }

                }

                override fun onChildRemoved(p0: DataSnapshot) {

                }

                override fun onChildMoved(p0: DataSnapshot, p1: String?) {

                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })

        }
    }
    // - - - ↑ 他のアクティビティから戻ってきたときの処理 ↑ - - -

    // - - - ↓ アクションバーを表示 ↓- - -
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // menuをInflateし、アクションバーを表示
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    // - - - ↑ アクションバーを表示 ↑- - -

    // - - - ↓ 縦の三点リーダーから「設定」をタップした時の処理 ↓ - - -
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            //右上のメニューから設定画面に進む
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }
    // - - - ↑ 縦の三点リーダーから「設定」をタップした時の処理 ↑ - - -

    // - - - ↓ ナビゲーションドロワーをタップした時の処理 ↓ - - -
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_hobby) {
            mFToolbar.title = "趣味"
            mGenre = 1
        } else if (id == R.id.nav_life) {
            mFToolbar.title = "生活"
            mGenre = 2
        } else if (id == R.id.nav_health) {
            mFToolbar.title = "健康"
            mGenre = 3
        } else if (id == R.id.nav_compter) {
            mFToolbar.title = "コンピューター"
            mGenre = 4
        } else if (id == R.id.nav_favorite) {
            mFToolbar.title = "お気に入り"
            mGenre = 5
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)

        // - - - ↓ お気に入り一覧画面に付随した記述 ↓- - -
        // ドロワーでジャンルが選択された時に、Firebaseに対してそのジャンルの質問のデータの変化を受け取るように、ChildEventListenerを設定
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mFAdapter.setQuestionArrayList(mQuestionArrayList)  // 変換したオブジェクトをAdapterにセットしてリストを更新
        mListView.adapter = mFAdapter

        // 選択したジャンルにリスナーを登録する
        if (mGenre == 5){
            val intent = Intent(applicationContext, FavoriteListActivity::class.java)
            startActivity(intent)

        } else if (mGenreRef != null && mGenre !== 5) {
            mGenreRef!!.removeEventListener(mEventListener)
        }
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        // addChildEventListenerメソッドを使って、Firebaseに対してそのジャンルの質問のデータの変化を受け取る
        mGenreRef!!.addChildEventListener(mEventListener)
        // - - - ↑ お気に入り一覧画面に付随した記述 ↑- - -

        return true
    }

    // - - - ↑ ナビゲーションドロワーをタップした時の処理 ↑ - - -

    override fun onBackPressed() {
        // バックキーの無効化
        moveTaskToBack (true)
    }

}

package com.example.qa_app

import java.io.Serializable
import java.util.ArrayList

// プロパティはvalで定義し、コンストラクタで値を設定
// Serializableクラスを実装している理由はIntentでデータを渡せるようにするため
class Question(val title: String, val body: String, val name: String, val uid: String, val questionUid: String, val genre: Int, bytes: ByteArray, val answers: ArrayList<Answer>) : Serializable {
    val imageBytes: ByteArray

    init {
        imageBytes = bytes.clone()
    }
}

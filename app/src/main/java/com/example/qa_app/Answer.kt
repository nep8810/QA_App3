package com.example.qa_app

import java.io.Serializable

// プロパティはvalで定義し、コンストラクタで値を設定
class Answer(val body: String, val name: String, val uid: String, val answerUid: String) : Serializable

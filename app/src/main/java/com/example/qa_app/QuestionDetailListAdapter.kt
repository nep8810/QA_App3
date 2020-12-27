package com.example.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class QuestionDetailListAdapter(context: Context, private val mQustion: Question) : BaseAdapter() {
    companion object {
        // 今回は質問と回答の2つのレイアウトを使うので2つの定数を用意(3つであればもう1つ定数を用意)
        private val TYPE_QUESTION = 0
        private val TYPE_ANSWER = 1
    }

    private var mLayoutInflater: LayoutInflater? = null

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return 1 + mQustion.answers.size
    }

    // getItemViewTypeメソッドは引数で渡ってきたポジションがどのタイプかを返す
    override fun getItemViewType(position: Int): Int {
        // ポジションが0の時に質問であるTYPE_QUESTIONを返し、それ以外は回答なのでTYPE_ANSWERを返す
        return if (position == 0) {
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Any {
        return mQustion
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        // getViewメソッドの中でgetItemViewTypeメソッドを呼び出してどちらのタイプか(TYPE_QUESTION or TYPE_ANSWER)を判断してレイアウトファイルを指定
        if (getItemViewType(position) == TYPE_QUESTION) {
            if (convertView == null) {
                // list_question_detail.xmlの各リストをコンバート
                convertView = mLayoutInflater!!.inflate(R.layout.list_question_detail, parent, false)!!
            }
            val body = mQustion.body
            val name = mQustion.name

            // bodyTextView = list_question_detail.xmlのTextView
            val bodyTextView = convertView.findViewById<View>(R.id.bodyTextView) as TextView
            bodyTextView.text = body

            // nameTextView = list_question_detail.xmlのTextView
            val nameTextView = convertView.findViewById<View>(R.id.nameTextView) as TextView
            nameTextView.text = name

            val bytes = mQustion.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                // imageView = list_question_detail.xmlのImageView
                val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
                imageView.setImageBitmap(image)
            }
        } else {
            if (convertView == null) {
                // list_answer.xmlの各リストをコンバート
                convertView = mLayoutInflater!!.inflate(R.layout.list_answer, parent, false)!!
            }

            val answer = mQustion.answers[position - 1]
            val body = answer.body
            val name = answer.name

            // bodyTextView = list_answer.xmlのTextView
            val bodyTextView = convertView.findViewById<View>(R.id.bodyTextView) as TextView
            bodyTextView.text = body

            // nameTextView = list_answer.xmlのTextView
            val nameTextView = convertView.findViewById<View>(R.id.nameTextView) as TextView
            nameTextView.text = name
        }

        return convertView
    }
}

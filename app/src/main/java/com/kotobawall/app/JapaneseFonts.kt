package com.kotobawall.app
import android.content.Context
import android.graphics.Typeface

object JapaneseFonts {
 private val fonts=mutableMapOf<String,Typeface>()
 @Synchronized fun get(context: Context,name: String,bold: Boolean): Typeface {
  val family=if(Typography.migrateFont(name)=="Mincho JP") "mincho" else "gothic"
  val key=family+if(bold) "_bold" else "_regular"
  return fonts.getOrPut(key) {Typeface.createFromAsset(context.assets,"fonts/$key.ttf")}
 }
}

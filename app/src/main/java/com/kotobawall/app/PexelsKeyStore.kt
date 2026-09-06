package com.kotobawall.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The API key is entered on-device, never bundled in the APK or source. */
class PexelsKeyStore(context: Context) {
 private val file=AtomicFile(File(context.noBackupFilesDir,"pexels-key.json"))
 private val alias="kotobawall.pexels.v1"
 private fun store()=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
 private fun encryptionKey(): SecretKey {
  (store().getKey(alias,null) as? SecretKey)?.let {return it}
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
   init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setRandomizedEncryptionRequired(true).build())
  }.generateKey()
 }
 @Synchronized fun read(): String {
  if(!file.baseFile.exists()) return ""
  check(file.baseFile.length()<=8192) {"Invalid saved credential."}
  val json=JSONObject(String(file.readFully(),Charsets.UTF_8))
  val key=store().getKey(alias,null) as? SecretKey ?: error("Saved credential is unavailable. Enter the key again.")
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,Base64.decode(json.getString("iv"),Base64.NO_WRAP)))
  val result=String(cipher.doFinal(Base64.decode(json.getString("data"),Base64.NO_WRAP)),Charsets.UTF_8)
  check(PexelsClient.validKey(result)) {"Invalid saved credential."}
  return result
 }
 @Synchronized fun write(value: String) {
  val raw=value.trim();require(PexelsClient.validKey(raw)) {"Enter a valid Pexels API key."}
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,encryptionKey())
  val encrypted=cipher.doFinal(raw.toByteArray(Charsets.UTF_8))
  val json=JSONObject().put("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP))
   .put("data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).toString()
  val output=file.startWrite()
  try {output.write(json.toByteArray(Charsets.UTF_8));file.finishWrite(output)}
  catch(e: Exception) {file.failWrite(output);throw e}
 }
 @Synchronized fun clear() {file.delete();store().deleteEntry(alias)}
}

package com.jakubjirak.logparser.premium.security

import com.jakubjirak.logparser.licensing.LicenseManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Premium Feature: Log Encryption
 * Encrypt/decrypt sensitive log data at rest
 */
class LogEncryption(private val licenseManager: LicenseManager) {
    
    private val algorithm = "AES/CBC/PKCS5Padding"
    private val keyAlgorithm = "PBKDF2WithHmacSHA256"
    
    fun encryptLog(content: String, password: String): EncryptedLog? {
        if (!licenseManager.isPremium()) {
            return null
        }
        
        try {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            
            val encrypted = cipher.doFinal(content.toByteArray())
            val checksum = calculateChecksum(content)
            
            return EncryptedLog(
                encryptedData = encrypted,
                salt = salt,
                iv = iv,
                checksum = checksum
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    fun decryptLog(encryptedLog: EncryptedLog, password: String): String? {
        if (!licenseManager.isPremium()) {
            return null
        }
        
        try {
            val key = deriveKey(password, encryptedLog.salt)
            val cipher = Cipher.getInstance(algorithm)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(encryptedLog.iv))
            
            val decrypted = cipher.doFinal(encryptedLog.encryptedData)
            val content = String(decrypted)
            
            if (calculateChecksum(content) != encryptedLog.checksum) {
                return null
            }
            
            return content
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance(keyAlgorithm)
        val key = factory.generateSecret(spec)
        return SecretKeySpec(key.encoded, "AES")
    }
    
    private fun calculateChecksum(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

data class EncryptedLog(
    val encryptedData: ByteArray,
    val salt: ByteArray,
    val iv: ByteArray,
    val checksum: String
)

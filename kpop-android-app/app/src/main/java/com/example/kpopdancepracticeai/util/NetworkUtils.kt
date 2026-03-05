package com.example.kpopdancepracticeai.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * 네트워크 상태를 확인하는 유틸리티 클래스입니다.
 * * 대용량의 전문가 영상을 다운로드하기 전, 사용자의 기기가
 * Wi-Fi에 연결되어 있는지, 모바일 데이터에 연결되어 있는지 판별하여
 * 적절한 UI(안내 문구 및 경고)를 띄우기 위해 사용됩니다.
 */
object NetworkUtils {

    /**
     * 현재 기기가 Wi-Fi 네트워크에 연결되어 있는지 확인합니다.
     * * @param context 안드로이드 Context
     * @return Wi-Fi 연결 시 true, 그 외의 경우 false 반환
     */
    fun isWifiConnected(context: Context): Boolean {
        // 시스템 서비스에서 ConnectivityManager를 가져옵니다.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 현재 활성화된 네트워크를 가져옵니다. 네트워크가 없으면 false를 반환합니다.
        val network = connectivityManager.activeNetwork ?: return false

        // 해당 네트워크의 특성(Capabilities)을 가져옵니다.
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        // 네트워크 전송 방식이 Wi-Fi인지 확인합니다.
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * 현재 기기가 모바일 데이터(셀룰러: LTE, 5G 등) 네트워크에 연결되어 있는지 확인합니다.
     * 대용량 다운로드 시 데이터 요금 경고를 띄우기 위해 사용합니다.
     *
     * @param context 안드로이드 Context
     * @return 모바일 데이터 연결 시 true, 그 외의 경우 false 반환
     */
    fun isMobileDataConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        // 네트워크 전송 방식이 셀룰러(모바일 데이터)인지 확인합니다.
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * 종류(Wi-Fi, 데이터 등)에 상관없이 현재 인터넷에 연결되어 통신이 가능한 상태인지 확인합니다.
     * * @param context 안드로이드 Context
     * @return 인터넷 사용 가능 시 true, 오프라인일 경우 false 반환
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Wi-Fi, 셀룰러, 이더넷 등 어떤 형태로든 인터넷이 연결되어 있으면 true를 반환합니다.
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
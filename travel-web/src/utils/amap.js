// 高德 JS API 加载器，保证只加载一次
let amapPromise = null

export function loadAmap() {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (amapPromise) {
    return amapPromise
  }

  // JS API 2.0 要求先设置安全密钥，否则报 INVALID_USER_SCODE
  window._AMapSecurityConfig = {
    securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE
  }

  amapPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/maps?v=2.0&key=' + import.meta.env.VITE_AMAP_KEY
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('高德地图加载失败'))
    document.head.appendChild(script)
  })

  return amapPromise
}

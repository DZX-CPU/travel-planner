<template>
  <div class="home">
    <div class="banner">
      <h1>让 AI 帮你规划<span>下一次旅行</span></h1>
      <p>输入目的地、天数和预算，几秒钟生成一份可以落地的行程</p>
      <el-button type="primary" size="large" round @click="$router.push('/generate')">开始定制行程</el-button>
      <div class="banner-tags">
        <span>✦ 15+ 热门城市</span>
        <span>✦ 按天时间轴安排</span>
        <span>✦ 地图路线可视化</span>
      </div>
    </div>

    <div class="section">
      <h3>热门城市</h3>
      <div class="city-grid">
        <div v-for="city in hotCities" :key="city.name" class="city-card" @click="goSpots(city.name)">
          <div class="city-name">{{ city.name }}</div>
          <div class="city-desc">{{ city.desc }}</div>
        </div>
      </div>
    </div>

    <div class="section">
      <h3>它能帮你做什么</h3>
      <div class="feature-grid">
        <div class="feature-card">
          <div class="feature-icon" style="background:#e0f2fe">AI</div>
          <b>行程智能定制</b>
          <p>结合真实景点数据生成行程，安排合理不折腾，预算控制贴着你的钱包来</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon" style="background:#ffedd5">地</div>
          <b>路线地图可视化</b>
          <p>每天的路线自动在地图上标点连线，起点终点清晰标注，方向一目了然</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon" style="background:#dcfce7">景</div>
          <b>景点数据查询</b>
          <p>基于高德地图数据，覆盖 15+ 热门城市的景点、地址和门票信息</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon" style="background:#fef9c3">记</div>
          <b>游记分享社区</b>
          <p>写下游记见闻，也可以参考其他旅行者的真实经历再做决定</p>
        </div>
      </div>
    </div>

    <div class="section steps-section">
      <h3>三步搞定一次旅行规划</h3>
      <div class="steps">
        <div class="step">
          <div class="step-no">1</div>
          <b>输入需求</b>
          <p>选目的地、天数、预算，勾几个偏好标签</p>
        </div>
        <div class="step-arrow">→</div>
        <div class="step">
          <div class="step-no">2</div>
          <b>AI 生成行程</b>
          <p>几秒钟拿到一份按天划分的完整安排</p>
        </div>
        <div class="step-arrow">→</div>
        <div class="step">
          <div class="step-no">3</div>
          <b>地图查看微调</b>
          <p>在地图上看路线走向，按时间轴细看每站</p>
        </div>
      </div>
    </div>

    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-brand">智游<span>助手</span></div>
        <p>基于大语言模型的智慧文旅行程规划系统 · 毕业设计作品</p>
        <p class="footer-links" @click="$router.push('/generate')">现在就去规划一次行程 →</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const hotCities = [
  { name: '杭州', desc: '西湖醋鱼与龙井茶' },
  { name: '成都', desc: '火锅、熊猫和慢生活' },
  { name: '西安', desc: '千年古都，碳水天堂' },
  { name: '北京', desc: '故宫长城，皇城根下' },
  { name: '厦门', desc: '鼓浪屿的海风' },
  { name: '重庆', desc: '8D魔幻山城' },
  { name: '南京', desc: '六朝古都的梧桐' },
  { name: '青岛', desc: '红瓦绿树与啤酒' }
]

function goSpots(city) {
  router.push({ path: '/spots', query: { city } })
}
</script>

<style scoped>
.banner {
  background:
    radial-gradient(circle at 80% 30%, rgba(14, 165, 233, 0.10) 0%, transparent 50%),
    radial-gradient(circle at 12% 85%, rgba(249, 115, 22, 0.07) 0%, transparent 45%),
    #fff;
  color: #0f172a;
  text-align: center;
  padding: 90px 20px 70px;
}
.banner h1 {
  font-size: 40px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 14px;
}
.banner h1 span {
  color: #0ea5e9;
}
.banner p {
  font-size: 16px;
  color: #64748b;
  margin-bottom: 28px;
}
.banner-tags {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 24px;
  font-size: 13px;
  color: #94a3b8;
}
.section {
  max-width: 1100px;
  margin: 48px auto;
  padding: 0 24px;
}
.section h3 {
  margin-bottom: 20px;
  font-size: 20px;
  font-weight: 600;
}
.city-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 14px;
}
.city-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px 14px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease-out;
  border: 1px solid #e2e8f0;
}
.city-card:hover {
  transform: translateY(-4px);
  border-color: #bae6fd;
  box-shadow: 0 12px 32px rgba(14, 165, 233, 0.12);
}
.city-name {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 6px;
}
.city-desc {
  font-size: 12px;
  color: #94a3b8;
}
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.feature-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 26px 22px;
  transition: all 0.25s ease-out;
}
.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}
.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  color: #0ea5e9;
  margin-bottom: 16px;
}
.feature-card b {
  display: block;
  font-size: 15px;
  margin-bottom: 8px;
}
.feature-card p {
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
}
.steps-section {
  padding-bottom: 60px;
}
.steps {
  display: flex;
  align-items: center;
  gap: 12px;
}
.step {
  flex: 1;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 26px 22px;
}
.step-no {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #0ea5e9;
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}
.step b {
  display: block;
  font-size: 15px;
  margin-bottom: 6px;
}
.step p {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}
.step-arrow {
  color: #cbd5e1;
  font-size: 22px;
  flex-shrink: 0;
}
.footer {
  border-top: 1px solid #e2e8f0;
  background: #fff;
  padding: 36px 20px 44px;
  text-align: center;
}
.footer-brand {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 10px;
}
.footer-brand span {
  color: #0ea5e9;
}
.footer p {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.8;
}
.footer-links {
  color: #0ea5e9;
  cursor: pointer;
}

@media (max-width: 900px) {
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .steps {
    flex-direction: column;
  }
  .step-arrow {
    transform: rotate(90deg);
  }
}
</style>

<template>
  <div class="generate-page">
    <div class="main-cols">
      <el-card class="form-col">
        <h3>AI 行程定制</h3>
        <p class="form-sub">填几个信息，AI 结合真实景点数据帮你排一份按天的行程</p>
        <el-form :model="form" label-width="80px" label-position="left" size="large">
          <el-form-item label="目的地">
            <el-select v-model="form.city" filterable placeholder="想去哪个城市？">
              <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="天数">
            <el-input-number v-model="form.days" :min="1" :max="7" />
          </el-form-item>
          <el-form-item label="人数">
            <el-input-number v-model="form.people" :min="1" :max="10" :value-on-clear="1" />
          </el-form-item>
          <el-form-item label="抵达时间">
            <el-radio-group v-model="form.arrivalTime">
              <el-radio-button value="上午">上午</el-radio-button>
              <el-radio-button value="下午">下午</el-radio-button>
              <el-radio-button value="晚上">晚上</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="预算">
            <el-input v-model="form.budget">
              <template #append>元</template>
            </el-input>
            <div v-if="perCapita" class="per-capita">人均约 ¥{{ perCapita }}</div>
          </el-form-item>
          <el-form-item label="偏好">
            <el-checkbox-group v-model="form.preferences">
              <el-checkbox value="亲子">亲子</el-checkbox>
              <el-checkbox value="美食">美食</el-checkbox>
              <el-checkbox value="文艺">文艺</el-checkbox>
              <el-checkbox value="历史">历史</el-checkbox>
              <el-checkbox value="自然风光">自然风光</el-checkbox>
              <el-checkbox value="穷游">穷游</el-checkbox>
              <el-checkbox value="拍照">拍照</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <!-- 按钮没有标签，表单项默认会按标签宽度留出左边距，单独置零再居中 -->
          <el-form-item label-width="0" class="btn-item">
            <el-button type="primary" size="large" :loading="generating" @click="onGenerate">
              {{ generating ? 'AI 正在规划行程…' : '生成行程' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-tips">
          <span>· 天数支持 1-7 天，行程会按天分组展示在地图上</span>
          <span>· 预算填全队总花费，不填也行；人数会用来分摊住宿和打车</span>
          <span>· 抵达时间只影响第一天，那天会先去住处放行李，节奏排得松一些</span>
          <span>· 偏好标签勾得越具体（比如"亲子+美食"），生成结果越合口味</span>
        </div>
      </el-card>

      <div class="preview-col">
        <div class="preview-card">
          <div class="preview-head">
            <b>生成出来大概长这样</b>
            <span>按天分组，每条都有时间和花费</span>
          </div>

          <div class="pv-body">
            <div class="pv-day">
              <div class="pv-day-head">
                <span class="pv-tag">Day 1</span>
                <span class="pv-theme">抵达日，先安顿下来</span>
              </div>
              <div class="pv-item"><i></i><em>15:00</em><span>回住处放行李</span></div>
              <div class="pv-item"><i></i><em>17:30</em><span>住处附近吃晚饭</span></div>
            </div>

            <div class="pv-day">
              <div class="pv-day-head">
                <span class="pv-tag">Day 2</span>
                <span class="pv-theme">市区经典线路</span>
              </div>
              <div class="pv-item"><i></i><em>09:00</em><span>逛第一个景点</span></div>
              <div class="pv-item"><i></i><em>12:00</em><span>本地馆子吃午饭</span></div>
              <div class="pv-item"><i></i><em>14:30</em><span>顺路逛第二个景点</span></div>
            </div>

            <div class="pv-day">
              <div class="pv-day-head">
                <span class="pv-tag">Day 3</span>
                <span class="pv-theme">收个尾再返程</span>
              </div>
              <div class="pv-item"><i></i><em>09:30</em><span>逛逛老街</span></div>
              <div class="pv-item"><i></i><em>12:30</em><span>最后一顿当地菜</span></div>
            </div>
          </div>

          <div class="pv-foot">
            <span>按天时间轴</span>
            <span>地图连线</span>
            <span>花费明细</span>
          </div>
        </div>
      </div>
    </div>

    <div class="faq-section">
      <h3>常见问题</h3>
      <div class="faq-grid">
        <div class="faq-card">
          <b>生成的行程能改吗？</b>
          <p>生成出来就是完整的一份。想调整就改改上面的条件重新生成，偏好标签多勾几个，结果会更贴近你要的。</p>
        </div>
        <div class="faq-card">
          <b>预算是怎么算的？</b>
          <p>填的是全队总花费，系统按人数摊成人均再交给 AI。门票和吃饭按人头算，住宿、打车按人数分摊。</p>
        </div>
        <div class="faq-card">
          <b>地图上的点是什么？</b>
          <p>只标真实景点。餐厅、酒店这些是 AI 推荐的位置，不在地图上打点，免得出现现实中不存在的地点。</p>
        </div>
        <div class="faq-card">
          <b>第一天为什么排得少？</b>
          <p>抵达日要放行李、吃饭，排满了走不下来。所以第一天会刻意排松一些，抵达越晚安排越少。</p>
        </div>
        <div class="faq-card">
          <b>住宿为什么只有一条？</b>
          <p>单城市的行程默认全程住同一家，不用天天搬行李，所以只出现一次。</p>
        </div>
        <div class="faq-card">
          <b>景点数据是哪来的？</b>
          <p>走的是高德地图的真实 POI 数据，先按城市查出来再交给 AI 挑选，所以景点名和位置都对得上。</p>
        </div>
      </div>
    </div>

    <el-dialog v-model="resultVisible" title="生成完成" width="500px">
      <p>{{ resultMsg }}</p>
      <template #footer>
        <el-button type="primary" @click="goDetail">查看行程</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const cities = ['北京', '上海', '杭州', '苏州', '南京', '成都', '重庆', '西安', '厦门', '广州', '深圳', '长沙', '武汉', '青岛', '三亚']

const form = reactive({
  city: '',
  days: 3,
  people: 2,
  arrivalTime: '下午',
  budget: '',
  preferences: []
})

// 预算按总人数填，顺手把人均算出来给用户看一眼
// 人数那格清空后是 null，total/null 算出 Infinity，v-if 挡不住，所以除数也要判
const perCapita = computed(() => {
  const total = Number(String(form.budget).replace(/[^0-9.]/g, ''))
  if (!total || !form.people) return 0
  return Math.floor(total / form.people)
})
const generating = ref(false)
const resultVisible = ref(false)
const resultMsg = ref('')
const generatedId = ref(null)

async function onGenerate() {
  if (!form.city) {
    ElMessage.warning('请选择目的地城市')
    return
  }
  // 预算比人数还小的时候，人均会算成 0，交给 AI 也排不出像样的行程
  const total = Number(String(form.budget).replace(/[^0-9.]/g, ''))
  if (form.budget && (!total || total < 100)) {
    ElMessage.warning('预算填得太少了，至少 100 元，或者留空不填')
    return
  }
  generating.value = true
  try {
    const plan = await request.post('/plan/generate', {
      city: form.city,
      days: form.days,
      people: form.people,
      arrivalTime: form.arrivalTime,
      budget: form.budget,
      preference: form.preferences.join(','),
      save: true
    })
    resultMsg.value = `《${plan.title}》已生成`
    generatedId.value = plan.id
    resultVisible.value = true
  } finally {
    generating.value = false
  }
}

function goDetail() {
  resultVisible.value = false
  router.push('/plan/' + generatedId.value)
}
</script>

<style scoped>
.generate-page {
  max-width: 1400px;
  margin: 28px auto;
  padding: 0 24px;
  min-height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
}
/* 上下按 7:3 分页面高度。basis 给 0，grow 才是真实比例；
   两边都保留默认的 min-height:auto，窗口太矮时内容不会被压扁 */
.main-cols {
  flex: 7;
  display: flex;
  gap: 24px;
  align-items: stretch;
}
.form-col {
  flex: 1;
  min-width: 0;
  display: flex;
}
.form-col :deep(.el-card) {
  flex: 1;
  padding: 8px 12px;
}
/* 卡片被撑高时把说明块顶到底部，空白留在按钮和说明之间，比留在卡片最下面自然 */
.form-col :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}
/* 一行一项；flex:1 + space-between 让每行在卡片高度里均匀分布，
   不用靠加大固定行距去凑 */
.form-col :deep(.el-form) {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.form-col :deep(.el-form-item) {
  margin-bottom: 18px;
}
.form-col :deep(.el-select),
.form-col :deep(.el-input),
.form-col :deep(.el-input-number) {
  width: 100%;
}
/* 数字框默认把数字居中，框一宽就很难看，跟其他输入框一样左对齐 */
.form-col :deep(.el-input-number .el-input__inner) {
  text-align: left;
}
/* 主按钮做得醒目一点，横跨整行居中 */
.form-col :deep(.btn-item .el-form-item__content) {
  justify-content: center;
}
.form-col :deep(.btn-item .el-button) {
  height: 52px;
  padding: 0 56px;
  font-size: 16px;
  letter-spacing: 1px;
}
.generate-page h3 {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 6px;
}
.form-sub {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}
.per-capita {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
}
/* 说明直接放进表单卡底部，不再单独占一个框 */
.form-tips {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px dashed #e2e8f0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 24px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.7;
}
.preview-col {
  width: 400px;
  flex-shrink: 0;
  display: flex;
}
.preview-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 22px 20px;
}
/* 标题和底部标签固定，三个 Day 块在中间铺开 */
.pv-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-evenly;
}
.preview-head {
  padding-bottom: 14px;
  margin-bottom: 16px;
  border-bottom: 1px dashed #e2e8f0;
}
.preview-head b {
  display: block;
  font-size: 15px;
  margin-bottom: 6px;
}
.preview-head span {
  font-size: 12px;
  color: #94a3b8;
}
.pv-day {
  margin-bottom: 0;
}
.pv-day-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.pv-tag {
  background: #0ea5e9;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}
.pv-theme {
  font-size: 12px;
  color: #64748b;
}
.pv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #475569;
  padding: 10px 0 10px 6px;
  border-left: 1px solid #e0f2fe;
  margin-left: 4px;
}
.pv-item i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #bae6fd;
  flex-shrink: 0;
  margin-left: -10px;
}
.pv-item em {
  font-style: normal;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}
.pv-foot {
  display: flex;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px dashed #e2e8f0;
}
.pv-foot span {
  flex: 1;
  text-align: center;
  font-size: 12px;
  color: #0ea5e9;
  background: #f0f9ff;
  border-radius: 8px;
  padding: 7px 0;
}
.faq-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  margin: 30px 0 40px;
}
.faq-grid {
  flex: 1;
}
.faq-section h3 {
  margin-bottom: 16px;
}
.faq-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.faq-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px 18px;
  transition: all 0.25s ease-out;
}
.faq-card:hover {
  transform: translateY(-4px);
  border-color: #bae6fd;
  box-shadow: 0 12px 32px rgba(14, 165, 233, 0.10);
}
.faq-card b {
  display: block;
  font-size: 14px;
  margin-bottom: 8px;
}
.faq-card p {
  font-size: 13px;
  color: #64748b;
  line-height: 1.75;
}
@media (max-width: 1100px) {
  /* 窄屏放不下两栏表单，退回单栏 */
  .form-col :deep(.el-form) {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 900px) {
  .main-cols {
    flex-direction: column;
  }
  .preview-col {
    width: 100%;
  }
  .faq-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 640px) {
  .faq-grid {
    grid-template-columns: 1fr;
  }
}
</style>

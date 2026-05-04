<template>
  <div id="app">
    <el-container style="min-height: 100vh">
      <el-aside width="220px" style="background: #304156">
        <div class="logo">
          <h2 style="color: #fff; text-align: center; padding: 16px 0">Spark 任务管理</h2>
        </div>
        <el-menu
          :default-active="$route.path"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
        >
          <el-menu-item index="/job/submit">
            <el-icon><Upload /></el-icon>
            <span>提交任务</span>
          </el-menu-item>
          <el-menu-item index="/job/list">
            <el-icon><List /></el-icon>
            <span>任务列表</span>
          </el-menu-item>
          <el-menu-item index="/dependency">
            <el-icon><Box /></el-icon>
            <span>依赖管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}
#app {
  height: 100vh;
}
</style>

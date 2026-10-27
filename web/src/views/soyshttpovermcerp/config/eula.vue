<template>
  <config-layout title="用户协议 (EULA)" :help-map="helpMap" file="EULA.yml" :dirty="dirty" :loading="loading" :saving="saving" @reload="reload" @save="save" >
      <el-card shadow="never">
        <el-alert type="info" :closable="false" style="margin-bottom:16px">
          根据 Mojang EULA，运行本插件代表您已接受 Minecraft EULA。此开关仅作记录，关闭时插件不会启用 Web 服务。
        </el-alert>
        <el-form label-width="160px" size="small">
          <el-form-item label="同意 EULA">
            <el-switch v-model="model.eula" active-text="已同意" inactive-text="未同意" />
            <div class="hint">对应 EULA.yml 中 eula: true / false。</div>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="mb">
        <div slot="header">使用与开发协议</div>
        <el-tabs>
          <el-tab-pane label="简体中文 (zh_cn)">
            <pre class="eula-text">【SOYSHTTPOverMC 使用与开发协议】

使用、开发并发布本插件，即视为您同意以下条款：

1. 禁止使用本插件从事任何违法犯罪活动，包括但不限于：

   （1）建设/跳转赌博、色情、毒品等违法网站或链接；

   （2）投放恐怖分子言论、煽动暴力等违法内容；

   （3）未经授权破坏、入侵他人计算机系统、网络或数据。

2. 禁止违反您所在国家/地区的法律法规使用本插件。

3. 因违规行为产生的一切法律责任，由使用者本人承担，插件作者不承担任何责任。</pre>
          </el-tab-pane>
          <el-tab-pane label="繁體中文 (zh_tw)">
            <pre class="eula-text">【SOYSHTTPOverMC 使用與開發協議】

使用、開發並發佈本插件，即視為您同意以下條款：

1. 禁止使用本插件從事任何違法犯罪活動，包括但不限於：

   （1）建設/跳轉賭博、色情、毒品等違法網站或連結；

   （2）投放恐怖分子言論、煽動暴力等違法內容；

   （3）未經授權破壞、入侵他人電腦系統、網路或資料。

2. 禁止違反您所在國家/地區的法律法規使用本插件。

3. 因違規行為產生的一切法律責任，由使用者本人承擔，插件作者不承擔任何責任。</pre>
          </el-tab-pane>
          <el-tab-pane label="English (en_us)">
            <pre class="eula-text">【SOYSHTTPOverMC USE AND DEVELOPMENT AGREEMENT (EULA)】

By using, developing, or distributing this plugin, you agree to the following terms:

1. You are prohibited from using this plugin for any illegal or criminal activity, including but not limited to:

   (1) Creating/redirecting links to illegal websites such as gambling, pornography, or drugs;

   (2) Spreading terrorist statements or content inciting violence;

   (3) Unauthorized disruption or intrusion into other people's computer systems, networks, or data.

2. You are prohibited from using this plugin in violation of the laws/regulations of your country or region.

3. All legal liability arising from any violation above shall be borne by the user. The plugin author shall not be liable.</pre>
          </el-tab-pane>
          <el-tab-pane label="日本語 (ja_jp)">
            <pre class="eula-text">【SOYSHTTPOverMC 利用・開発契約（EULA）】

本プラグインを利用・開発・頒布する場合、以下の条項に同意するものとみなされます。

1. 本プラグインを、違法または犯罪行為に使用することを禁止します（ただし以下に限りません）：

   （1）ギャンブル・ポルノ・薬物等の違法サイトの作成／リダイレクト；

   （2）テロリストの発言や暴力を煽るコンテンツの配信；

   （3）他人のコンピュータシステム・ネットワーク・データへの無断改変・侵入。

2. お住まいの国・地域の法令に違反する形で本プラグインを使用することを禁止します。

3. 上記違反によって生じた法的責任はすべて利用者が負担します。プラグイン作者は責任を負いません。</pre>
          </el-tab-pane>
          <el-tab-pane label="한국어 (ko_kr)">
            <pre class="eula-text">【SOYSHTTPOverMC 이용 및 개발 계약(EULA)】

본 플러그인을 사용·개발·배포하는 경우 아래 조항에 동의한 것으로 간주합니다.

1. 본 플러그인을 불법 또는 범죄 활동에 사용하는 것을 금지합니다（이에 국한되지 않음）：

   （1）도박·음란·마약 등의 불법 사이트 구축/연결；

   （2）테러리스트 발언이나 폭력 선동 콘텐츠 유포；

   （3）타인의 컴퓨터 시스템·네트워크·데이터를 무단으로 파괴 또는 침입.

2. 거주 국가/지역의 법률·규정을 위반하여 본 플러그인을 사용하는 것을 금지합니다.

3. 위반으로 인해 발생한 모든 법적 책임은 이용자가 부담합니다. 플러그인 작성자는 책임을 지지 않습니다.</pre>
          </el-tab-pane>
        </el-tabs>
      </el-card>
  </config-layout>
</template>

<script>
import configPage from '../mixins/configPage'
import ConfigLayout from '../components/ConfigLayout.vue'

export default {
  name: 'SettingsEula',
  mixins: [configPage],
  components: { ConfigLayout },
  data() {
    return {
      fileId: 'eula',
      helpMap: {
        '同意 EULA': '根据 Mojang EULA，运行本插件代表您已接受 Minecraft EULA。此开关仅作记录，关闭时插件不会启用 Web 服务。对应 EULA.yml 中 eula: true / false。开启后插件才会初始化并对外提供 Web/HTTP 服务；关闭则插件保持禁用 Web 功能的保守状态，避免在未获授权的情况下运行服务。'
      }
    }
  }
}
</script>

<style scoped>
.hint { font-size: 12px; color: #a8abb2; line-height: 1.5; }
.eula-text {
  margin: 0; padding: 8px 4px;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
  font-size: 13px; color: #303133; line-height: 1.8;
  white-space: pre-wrap; word-break: break-word;
  background: #fafafa; border-radius: 4px;
}
</style>

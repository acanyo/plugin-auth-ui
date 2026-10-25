/** Ready-made HTML blocks the studio can insert into a design slot. */
export interface Snippet {
  label: string
  html: string
}

export const SLOT_SNIPPETS: Record<'aside' | 'header' | 'footer', readonly Snippet[]> = {
  aside: [
    {
      label: '标题',
      html: `<span class="auth-chip" data-animate="fade">欢迎回来</span>
<div class="auth-spacer"></div>
<p class="auth-big" data-animate="rise" data-delay="120">继续你的<br>阅读与创作</p>`,
    },
    {
      label: '亮点列表',
      html: `<ul class="auth-features" data-animate="rise" data-delay="240">
  <li>一个账号，跨设备同步</li>
  <li>评论、收藏与订阅随时可用</li>
  <li>数据安全，随时可导出</li>
</ul>`,
    },
    {
      label: '数据',
      html: `<div class="auth-stats" data-animate="rise" data-delay="360">
  <div><strong>12k+</strong><span>位读者</span></div>
  <div><strong>800+</strong><span>篇文章</span></div>
</div>`,
    },
    {
      label: '引言',
      html: `<blockquote class="auth-quote" data-animate="fade" data-delay="300">把时间花在美好的事物上。</blockquote>`,
    },
    {
      label: '图片',
      html: `<img src="/upload/your-image.png" alt="" style="width:100%;border-radius:16px" data-animate="zoom">`,
    },
  ],
  header: [
    { label: '标签', html: `<span class="auth-chip" data-animate="fade">✨ 新功能上线</span>` },
    {
      label: '提示',
      html: `<p class="auth-chip" style="width:100%;justify-content:center" data-animate="fade">登录后可同步你的收藏与评论</p>`,
    },
  ],
  footer: [
    { label: '协议', html: `<p>登录即表示同意 <a href="/terms">服务条款</a> 与 <a href="/privacy">隐私政策</a></p>` },
    { label: '联系', html: `<p>遇到问题？<a href="mailto:hi@example.com">联系站长</a></p>` },
  ],
}

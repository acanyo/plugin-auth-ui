import type { McpTool } from './mcp-status'

/**
 * The tools this plugin contributes, shown before MCP Server is installed. Once it runs, the page
 * lists the live catalog instead. Keep in sync with AuthUiMcpToolProvider.
 */
export const AUTH_UI_TOOLS: readonly McpTool[] = [
  { name: 'auth-ui__get_design_guide', title: '获取设计指南', description: '返回登录界面设计格式、页面结构、CSS 变量、动效，以及 AI 必须遵循的设计流程。', readOnly: true, destructive: false },
  { name: 'auth-ui__list_designs', title: '列出登录设计', description: '列出所有登录界面设计及当前启用的设计。', readOnly: true, destructive: false },
  { name: 'auth-ui__get_design', title: '获取登录设计', description: '读取一个设计的完整内容与版本号。', readOnly: true, destructive: false },
  { name: 'auth-ui__lint_design', title: '检查登录设计', description: '检查设计内容中会被忽略或过滤的部分，不会保存。', readOnly: true, destructive: false },
  { name: 'auth-ui__create_design', title: '创建登录设计', description: '新建一个登录界面设计，不会自动启用。', readOnly: false, destructive: false },
  { name: 'auth-ui__update_design', title: '更新登录设计', description: '整体替换一个设计的内容；若该设计已启用，站点立即生效。', readOnly: false, destructive: false },
  { name: 'auth-ui__delete_design', title: '删除登录设计', description: '删除一个设计；若该设计正在使用，站点恢复默认样式。', readOnly: false, destructive: true },
  { name: 'auth-ui__activate_design', title: '启用登录设计', description: '把设计应用到全站登录、注册与退出页面；传空字符串恢复默认样式。', readOnly: false, destructive: false },
  { name: 'auth-ui__preview_design', title: '预览登录设计', description: '返回设计在各页面（独立页与弹窗）的预览链接。', readOnly: true, destructive: false },
]

/** Requests to try once a client is connected. */
export const EXAMPLE_PROMPTS: readonly string[] = [
  '给我的博客设计一个深色赛博朋克风格的登录页，左边放品牌介绍，背景要有极光动效，先给我预览链接。',
  '参考现在启用的登录设计，做一个更简洁的浅色版本，表单逐项浮现，不要直接启用。',
  '把当前登录页的主色改成 #e11d48，其他保持不变。',
  '检查所有登录设计里有没有会被过滤掉的 HTML 或无效颜色，并帮我修好。',
]

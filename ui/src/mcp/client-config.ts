/** Client setup shown on the AI page, following Halo MCP Server's README. */
export const TOKEN_ENV = 'HALO_MCP_TOKEN'
/** Served by the plugin from its resources; see build.gradle and integration.yaml. */
export const SKILL_PATH = '/plugins/auth-ui/assets/mcp/skill/SKILL.md'
export const GUIDE_PATH = '/plugins/auth-ui/assets/mcp/design-guide.md'

export interface ClientConfig {
  id: string
  label: string
  /** Where the snippet goes. */
  file: string
  snippet: string
}

export function mcpEndpoint(origin: string): string {
  return `${origin.replace(/\/+$/, '')}/mcp`
}

export function tokenExport(): string {
  return `export ${TOKEN_ENV}='hmcp_粘贴你的访问密钥'`
}

export function clientConfigs(origin: string): ClientConfig[] {
  const endpoint = mcpEndpoint(origin)
  return [
    {
      id: 'claude-code',
      label: 'Claude Code',
      file: '项目根目录的 .mcp.json',
      snippet: JSON.stringify(
        {
          mcpServers: {
            halo: {
              type: 'http',
              url: endpoint,
              headers: { Authorization: `Bearer \${${TOKEN_ENV}}` },
            },
          },
        },
        null,
        2,
      ),
    },
    {
      id: 'codex',
      label: 'Codex',
      file: '~/.codex/config.toml',
      snippet: `[mcp_servers.halo]\nurl = "${endpoint}"\nbearer_token_env_var = "${TOKEN_ENV}"`,
    },
  ]
}

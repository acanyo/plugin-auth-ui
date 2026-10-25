import { describe, expect, it } from 'vitest'

import { clientConfigs, mcpEndpoint } from '../client-config'
import { countKeysWithAccess, ownTools, serverState } from '../mcp-status'
import { AUTH_UI_TOOLS } from '../tools'

describe('serverState', () => {
  it('reads the plugin lifecycle', () => {
    expect(serverState(undefined)).toBe('missing')
    expect(serverState({ spec: { enabled: false }, status: { phase: 'STOPPED' } })).toBe('disabled')
    expect(serverState({ spec: { enabled: true }, status: { phase: 'RESOLVED' } })).toBe('starting')
    expect(serverState({ spec: { enabled: true }, status: { phase: 'STARTED' } })).toBe('ready')
    expect(serverState({ spec: { enabled: true }, status: { phase: 'FAILED' } })).toBe('failed')
  })
})

describe('countKeysWithAccess', () => {
  const tools = ['auth-ui__list_designs', 'auth-ui__create_design']

  it('counts enabled keys that select one of the tools or all tools', () => {
    expect(
      countKeysWithAccess(
        [
          { enabled: true, allowedTools: ['auth-ui__list_designs'] },
          { enabled: true, allowedTools: ['*'] },
          { enabled: false, allowedTools: ['auth-ui__create_design'] },
          { enabled: true, allowedTools: ['halo_search_content'] },
        ],
        tools,
      ),
    ).toBe(2)
  })
})

describe('ownTools', () => {
  it('keeps only tools contributed by this plugin', () => {
    const tool = { title: 't', description: 'd', readOnly: true, destructive: false }
    expect(
      ownTools([
        { ...tool, name: 'auth-ui__list_designs', source: { pluginName: 'auth-ui' } },
        { ...tool, name: 'halo_search_content', source: { pluginName: 'mcp-server' } },
      ]).map((item) => item.name),
    ).toEqual(['auth-ui__list_designs'])
  })

  it('lists the same tools offline as the server provides', () => {
    expect(AUTH_UI_TOOLS.map((tool) => tool.name)).toContain('auth-ui__get_design_guide')
    expect(AUTH_UI_TOOLS.every((tool) => tool.name.startsWith('auth-ui__'))).toBe(true)
  })
})

describe('client configuration', () => {
  it('points clients at the site MCP endpoint without embedding a key', () => {
    expect(mcpEndpoint('https://blog.example.com/')).toBe('https://blog.example.com/mcp')
    const [claude, codex] = clientConfigs('https://blog.example.com')
    expect(JSON.parse(claude!.snippet).mcpServers.halo).toEqual({
      type: 'http',
      url: 'https://blog.example.com/mcp',
      headers: { Authorization: 'Bearer ${HALO_MCP_TOKEN}' },
    })
    expect(codex!.snippet).toContain('bearer_token_env_var = "HALO_MCP_TOKEN"')
  })
})

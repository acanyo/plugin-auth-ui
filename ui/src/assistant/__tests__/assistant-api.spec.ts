import { describe, expect, it, vi } from 'vitest'

vi.mock('@halo-dev/api-client', () => ({ axiosInstance: {} }))

import { errorMessage } from '../assistant-api'

describe('errorMessage', () => {
  it('prefers the server message, then the problem detail', () => {
    expect(errorMessage({ response: { data: { message: '没有默认模型', detail: 'x' } } })).toBe('没有默认模型')
    expect(errorMessage({ response: { data: { detail: '请求无效' } } })).toBe('请求无效')
  })

  it('explains client timeouts', () => {
    expect(errorMessage({ code: 'ECONNABORTED' })).toContain('等待时间过长')
  })

  it('falls back to a generic message', () => {
    expect(errorMessage(new Error('network'))).toBe('AI 设计暂时不可用，请稍后重试。')
    expect(errorMessage(undefined)).toBe('AI 设计暂时不可用，请稍后重试。')
  })
})

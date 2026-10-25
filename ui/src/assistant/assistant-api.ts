import { axiosInstance } from '@halo-dev/api-client'

import type { TemplateDraft } from '../templates/types'

const BASE = '/apis/api.authui.xhhao.com/v1alpha1'
/** Halo's console skips its error toast for requests marked `mute`; the panel shows errors itself. */
const MUTED = { mute: true } as Record<string, unknown>
/** A full design takes a while to generate; the server stops at 150 seconds per model call. */
const TIMEOUT = 6 * 60 * 1000

export interface AssistantStatus {
  available: boolean
  /** AI Foundation is installed, so the user can pick a model even if the chosen one is unusable. */
  canChooseModel: boolean
  reason: string
  /** Where to fix an unavailable assistant: the AI Foundation settings or the app store. */
  setupUrl: string
}

export interface SuggestionRequest {
  design: TemplateDraft
  instruction: string
  /** Earlier requests in the conversation, oldest first. */
  history: string[]
  allowScript: boolean
  /** AI Foundation model name; empty uses the default language model. */
  modelName: string
}

export interface Suggestion {
  design: TemplateDraft
  summary: string
  /** Top-level fields that changed, e.g. `theme`, `code`. */
  changed: string[]
  /** Problems the model could not fix; the renderer will drop these parts. */
  warnings: string[]
  model: string
  tokens: number | null
  turns: number
}

export async function fetchAssistantStatus(modelName = ''): Promise<AssistantStatus> {
  const { data } = await axiosInstance.get<AssistantStatus>(`${BASE}/assistant`, {
    ...MUTED,
    params: modelName ? { modelName } : undefined,
  })
  return data
}

export async function requestSuggestion(request: SuggestionRequest, signal?: AbortSignal): Promise<Suggestion> {
  const { data } = await axiosInstance.post<Suggestion>(`${BASE}/suggestions`, request, {
    ...MUTED,
    signal,
    timeout: TIMEOUT,
  })
  return data
}

/** The server's explanation for a failed request, or a generic message. */
export function errorMessage(error: unknown): string {
  const data = (error as { response?: { data?: { message?: string; detail?: string } } })?.response?.data
  if (data?.message) return data.message
  if (data?.detail) return data.detail
  if ((error as { code?: string })?.code === 'ECONNABORTED') return '等待时间过长，请把需求拆小一些再试。'
  return 'AI 设计暂时不可用，请稍后重试。'
}

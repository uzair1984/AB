export function buildSessionUpdate({ voice = 'alloy', instructions } = {}) {
  return {
    type: 'session.update',
    session: {
      type: 'realtime',
      output_modalities: ['audio'],
      instructions: instructions || [
        'You are AB AgentX, a helpful live phone-call assistant and interpreter.',
        "Detect the caller's spoken language automatically and respond in that same language unless asked to translate.",
        'Speak naturally and concisely, as this is a telephone call.',
        'If asked to interpret between two people, translate faithfully without adding opinions.',
        'Do not claim to be the human who owns the phone. Say you are AB AgentX.',
        'Ask one short clarification question when the request is unclear.'
      ].join(' '),
      audio: {
        input: {
          format: { type: 'audio/pcmu' },
          transcription: { model: 'gpt-4o-mini-transcribe' },
          turn_detection: { type: 'server_vad', threshold: 0.5, prefix_padding_ms: 300, silence_duration_ms: 650 }
        },
        output: { format: { type: 'audio/pcmu' }, voice }
      },
      temperature: 0.6,
      max_response_output_tokens: 180
    }
  };
}

export function parseTwilioMessage(raw) {
  const message = typeof raw === 'string' ? JSON.parse(raw) : raw;
  if (!message || typeof message.event !== 'string') throw new Error('Invalid Twilio media message: missing event');
  if (message.event === 'start' && !message.start?.streamSid) throw new Error('Twilio start event is missing streamSid');
  if (message.event === 'media' && (!message.media?.payload || !message.streamSid)) throw new Error('Twilio media event is missing streamSid or payload');
  return message;
}

export function twilioMediaFrame(streamSid, payload) {
  if (!streamSid || !payload) throw new Error('streamSid and payload are required');
  return { event: 'media', streamSid, media: { payload } };
}

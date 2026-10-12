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
      max_output_tokens: 180
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

export function buildInitialGreetingEvents() {
  return [
    {
      type: 'conversation.item.create',
      item: {
        type: 'message',
        role: 'user',
        content: [{
          type: 'input_text',
          text: 'Begin this call by greeting the caller in the language they used if it is already clear; otherwise greet briefly in English. Say exactly in meaning: “Hello, I am AB AgentX, an AI assistant. The person you called is unavailable right now. How can I help?” Then listen for their reply.'
        }]
      }
    },
    { type: 'response.create', response: { output_modalities: ['audio'] } }
  ];
}

export function buildVoiceTwiML({ streamUrl, token }) {
  if (!streamUrl || !token) throw new Error('streamUrl and token are required');
  const escapeXml = value => String(value).replace(/[<>&"']/g, char => ({
    '<': '&lt;', '>': '&gt;', '&': '&amp;', '"': '&quot;', "'": '&apos;'
  })[char]);
  const url = new URL(streamUrl);
  if (url.protocol !== 'wss:' || url.search || url.hash) throw new Error('streamUrl must be a wss URL without query or fragment');
  return `<?xml version="1.0" encoding="UTF-8"?><Response><Connect><Stream url="${escapeXml(url.toString())}"><Parameter name="token" value="${escapeXml(token)}" /></Stream></Connect></Response>`;
}

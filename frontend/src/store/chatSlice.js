import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

let msgId = 0;

export const sendMessage = createAsyncThunk(
  'chat/sendMessage',
  async (content, { dispatch }) => {
    dispatch(addMessage({ id: ++msgId, role: 'user', content }));

    const assistantId = ++msgId;
    dispatch(addMessage({ id: assistantId, role: 'assistant', content: '' }));
    dispatch(setStreaming(true));

    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: content }),
    });

    if (!response.ok) throw new Error(`BFF error: ${response.status}`);

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      for (const line of chunk.split('\n')) {
        if (!line.startsWith('data: ')) continue;
        const data = line.slice(6).trim();
        if (data === '[DONE]') return;
        dispatch(appendToken({ id: assistantId, token: data }));
      }
    }
  }
);

const chatSlice = createSlice({
  name: 'chat',
  initialState: {
    messages: [],
    streaming: false,
    error: null,
  },
  reducers: {
    addMessage(state, action) {
      state.messages.push(action.payload);
    },
    appendToken(state, action) {
      const msg = state.messages.find((m) => m.id === action.payload.id);
      if (msg) msg.content += action.payload.token;
    },
    setStreaming(state, action) {
      state.streaming = action.payload;
    },
    clearChat(state) {
      state.messages = [];
      state.streaming = false;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(sendMessage.fulfilled, (state) => {
        state.streaming = false;
        state.error = null;
      })
      .addCase(sendMessage.rejected, (state, action) => {
        state.streaming = false;
        state.error = action.error.message;
      });
  },
});

export const { addMessage, appendToken, setStreaming, clearChat } = chatSlice.actions;
export default chatSlice.reducer;

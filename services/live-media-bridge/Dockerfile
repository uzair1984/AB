FROM node:22-bookworm-slim
ENV NODE_ENV=production
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev && npm cache clean --force
COPY --chown=node:node protocol.js server.js ./
USER node
EXPOSE 8080
CMD ["npm", "start"]

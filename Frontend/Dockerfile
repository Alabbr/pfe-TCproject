# Stage 1: Build Angular App
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --legacy-peer-deps
COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine
# Copier la conf Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copier les fichiers buildés
COPY --from=build /app/dist/tc-hub/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

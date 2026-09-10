Write-Host "============================================="
Write-Host "   PFE DevOps - Build & Deploy Kubernetes    "
Write-Host "============================================="

Write-Host "`n[1/5] Building ML-Service image..."
cd ML-Service
docker build -t pfe-ml-service:latest .
cd ..

Write-Host "`n[2/5] Building Backend image..."
cd Backend/TC-Project
docker build -t pfe-backend:latest .
cd ../..

Write-Host "`n[3/5] Building Frontend image..."
cd Frontend
docker build -t pfe-frontend:latest .
cd ..

Write-Host "`n[4/5] Applying Kubernetes configurations..."
kubectl apply -f Backend/k8s/namespace.yaml
kubectl apply -f Backend/k8s/postgres.yaml
kubectl apply -f Backend/k8s/ml-service.yaml
kubectl apply -f Backend/k8s/backend.yaml
kubectl apply -f Backend/k8s/frontend.yaml

Write-Host "`n[5/5] Deployment complete! Waiting for pods to start..."
Start-Sleep -Seconds 5
kubectl get pods -n pfe-app

Write-Host "`nAccess your application at http://localhost"
Write-Host "To expose with ngrok run: ngrok http 80"

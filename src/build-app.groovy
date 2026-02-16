name: Build Hubitat Apps
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup Groovy
      uses: roryprimrose/setup-groovy@v1
      with:
        groovy-version: '2.4.21'  # Hubitat version
    
    - name: Build Apps
      run: |
        # Make executable
        chmod +x src/build-app.groovy
        
        # Run build (concat src/ → apps/)
        groovy src/build-app.groovy
        
        # Verify outputs
        ls -la apps/
        ls -la drivers/
    
    - name: Commit Deployables
      if: github.ref == 'refs/heads/main'
      run: |
        git config --local user.email "action@github.com"
        git config --local user.name "GitHub Action"
        git add apps/*.groovy drivers/*.groovy
        git diff --staged --quiet || git commit -m "Auto-build: Update deployable files"
        git push
    
    - name: Update HPM Repo List
      if: github.ref == 'refs/heads/main'
      run: |
        # Optional: ping HPM repo list
        echo "New WeatherSense build complete: $GITHUB_SHA"

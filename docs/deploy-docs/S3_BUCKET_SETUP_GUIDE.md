# AWS S3 버킷 설정 가이드

## 📋 목차

1. [S3 버킷 구성 개요](#s3-버킷-구성-개요)
2. [CodeDeploy 배포용 버킷](#codedeploy-배포용-버킷)
3. [이미지 저장용 버킷](#이미지-저장용-버킷)
4. [IAM 권한 설정](#iam-권한-설정)
5. [애플리케이션 연동](#애플리케이션-연동)

---

## 🏗️ S3 버킷 구성 개요

KoSpot 프로젝트는 **2개의 S3 버킷**을 사용합니다:

### 버킷 목록

| 버킷명 | 용도 | 접근 주체 | 리전 |
|--------|------|-----------|------|
| `kospot-deploy-bucket` | CI/CD 배포 패키지 저장 | GitHub Actions, CodeDeploy, EC2 | ap-northeast-2 |
| `kospot-images-prod` | 애플리케이션 이미지 저장 | EC2 (Spring Boot 애플리케이션) | ap-northeast-2 |

### 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                     GitHub Actions                           │
│                  (빌드 & 배포 파이프라인)                     │
└────────────────────────┬────────────────────────────────────┘
                         │ 빌드 아티팩트 업로드
                         ▼
        ┌────────────────────────────────────────┐
        │  kospot-deploy-bucket                  │
        │  (CodeDeploy 전용)                     │
        │  ├── deploy-abc123.zip                 │
        │  ├── deploy-def456.zip                 │
        │  └── ...                               │
        └────────┬───────────────────────────────┘
                 │ CodeDeploy가 패키지 가져옴
                 ▼
        ┌────────────────────────────────────────┐
        │         EC2 Instance                    │
        │  ┌──────────────────────────────────┐  │
        │  │   Spring Boot Application        │  │
        │  └──────────┬───────────────────────┘  │
        └─────────────┼──────────────────────────┘
                      │ 이미지 업로드/다운로드
                      ▼
        ┌────────────────────────────────────────┐
        │  kospot-images-prod                    │
        │  (애플리케이션 이미지)                  │
        │  ├── banners/                          │
        │  │   ├── banner-001.jpg                │
        │  │   └── banner-002.png                │
        │  ├── photomode/                        │
        │  │   ├── photo-12345.jpg               │
        │  │   └── photo-67890.jpg               │
        │  ├── items/                            │
        │  │   ├── item-speedboost.png           │
        │  │   └── item-shield.png               │
        │  └── profiles/                         │
        │      └── user-avatar-*.jpg             │
        └────────────────────────────────────────┘
                      │
                      ▼
        ┌────────────────────────────────────────┐
        │     CloudFront (선택사항)              │
        │     (CDN으로 이미지 전송 가속화)        │
        └────────────────────────────────────────┘
```

---

## 📦 CodeDeploy 배포용 버킷

### 1. 버킷 생성

#### AWS Console을 통한 생성

1. AWS Console → S3 → **Create bucket**
2. 설정:
   - **Bucket name**: `kospot-deploy-bucket`
   - **Region**: `ap-northeast-2` (Asia Pacific - Seoul)
   - **Block Public Access**: 모두 체크 (퍼블릭 액세스 차단)
   - **Bucket Versioning**: Disabled
   - **Default encryption**: SSE-S3
3. **Create bucket** 클릭

#### AWS CLI를 통한 생성

```bash
# 버킷 생성
aws s3 mb s3://kospot-deploy-bucket --region ap-northeast-2

# 퍼블릭 액세스 차단
aws s3api put-public-access-block \
  --bucket kospot-deploy-bucket \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 암호화 활성화
aws s3api put-bucket-encryption \
  --bucket kospot-deploy-bucket \
  --server-side-encryption-configuration \
    '{"Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"},"BucketKeyEnabled": true}]}'
```

### 2. 수명 주기 정책 설정

배포 패키지는 임시 파일이므로 30일 후 자동 삭제하도록 설정합니다.

#### lifecycle-policy.json 파일 생성

```json
{
  "Rules": [
    {
      "Id": "DeleteOldDeployments",
      "Status": "Enabled",
      "Prefix": "deploy-",
      "Expiration": {
        "Days": 30
      },
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 7
      },
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 7
      }
    }
  ]
}
```

#### 정책 적용

```bash
aws s3api put-bucket-lifecycle-configuration \
  --bucket kospot-deploy-bucket \
  --lifecycle-configuration file://lifecycle-policy.json
```

### 3. 버킷 정책 설정

#### bucket-policy.json 파일 생성

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowCodeDeployAccess",
      "Effect": "Allow",
      "Principal": {
        "Service": "codedeploy.amazonaws.com"
      },
      "Action": [
        "s3:GetObject",
        "s3:GetObjectVersion"
      ],
      "Resource": "arn:aws:s3:::kospot-deploy-bucket/*"
    },
    {
      "Sid": "AllowEC2InstanceAccess",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::YOUR_ACCOUNT_ID:role/KoSpotEC2CodeDeployRole"
      },
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-deploy-bucket",
        "arn:aws:s3:::kospot-deploy-bucket/*"
      ]
    }
  ]
}
```

⚠️ `YOUR_ACCOUNT_ID`를 실제 AWS 계정 ID로 변경하세요.

#### 정책 적용

```bash
aws s3api put-bucket-policy \
  --bucket kospot-deploy-bucket \
  --policy file://bucket-policy.json
```

---

## 🖼️ 이미지 저장용 버킷

### 1. 버킷 생성

#### AWS Console을 통한 생성

1. AWS Console → S3 → **Create bucket**
2. 설정:
   - **Bucket name**: `kospot-images-prod`
   - **Region**: `ap-northeast-2`
   - **Block Public Access**: 모두 체크 (Presigned URL 사용)
   - **Bucket Versioning**: Enabled (이미지 복구 가능)
   - **Default encryption**: SSE-S3
3. **Create bucket** 클릭

#### AWS CLI를 통한 생성

```bash
# 버킷 생성
aws s3 mb s3://kospot-images-prod --region ap-northeast-2

# 버저닝 활성화
aws s3api put-bucket-versioning \
  --bucket kospot-images-prod \
  --versioning-configuration Status=Enabled

# 퍼블릭 액세스 차단 (Presigned URL 사용)
aws s3api put-public-access-block \
  --bucket kospot-images-prod \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 암호화 활성화
aws s3api put-bucket-encryption \
  --bucket kospot-images-prod \
  --server-side-encryption-configuration \
    '{"Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"},"BucketKeyEnabled": true}]}'
```

### 2. 폴더 구조 생성

```bash
# S3에 폴더 구조 생성 (빈 객체 업로드)
aws s3api put-object --bucket kospot-images-prod --key banners/
aws s3api put-object --bucket kospot-images-prod --key photomode/
aws s3api put-object --bucket kospot-images-prod --key items/
aws s3api put-object --bucket kospot-images-prod --key profiles/
aws s3api put-object --bucket kospot-images-prod --key thumbnails/
```

**폴더 구조**:
```
kospot-images-prod/
├── banners/           # 메인 페이지 배너 이미지
├── photomode/         # 포토모드에서 사용할 배경/스티커 이미지
├── items/             # 게임 아이템 이미지
├── profiles/          # 사용자 프로필 이미지
└── thumbnails/        # 썸네일 이미지 (자동 생성)
```

### 3. CORS 설정 (프론트엔드에서 직접 업로드하는 경우)

#### cors-config.json 파일 생성

```json
{
  "CORSRules": [
    {
      "AllowedOrigins": [
        "https://kospot.example.com",
        "https://www.kospot.example.com"
      ],
      "AllowedMethods": [
        "GET",
        "PUT",
        "POST",
        "DELETE",
        "HEAD"
      ],
      "AllowedHeaders": [
        "*"
      ],
      "ExposeHeaders": [
        "ETag",
        "x-amz-request-id"
      ],
      "MaxAgeSeconds": 3000
    }
  ]
}
```

#### CORS 정책 적용

```bash
aws s3api put-bucket-cors \
  --bucket kospot-images-prod \
  --cors-configuration file://cors-config.json
```

### 4. 수명 주기 정책 (썸네일 자동 삭제 - 선택사항)

```json
{
  "Rules": [
    {
      "Id": "DeleteOldThumbnails",
      "Status": "Enabled",
      "Prefix": "thumbnails/",
      "Expiration": {
        "Days": 90
      }
    },
    {
      "Id": "TransitionOldVersions",
      "Status": "Enabled",
      "Prefix": "",
      "NoncurrentVersionTransitions": [
        {
          "NoncurrentDays": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "NoncurrentDays": 90,
          "StorageClass": "GLACIER"
        }
      ],
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 180
      }
    }
  ]
}
```

---

## 🔐 IAM 권한 설정

### 1. EC2 인스턴스 역할

EC2 인스턴스가 양쪽 버킷에 접근할 수 있도록 IAM 역할을 설정합니다.

#### IAM Policy 생성

**정책 이름**: `KoSpotS3AccessPolicy`

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CodeDeployBucketReadOnly",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:GetObjectVersion",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-deploy-bucket",
        "arn:aws:s3:::kospot-deploy-bucket/*"
      ]
    },
    {
      "Sid": "ImagesBucketFullAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:PutObjectAcl",
        "s3:GetObjectVersion"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-images-prod",
        "arn:aws:s3:::kospot-images-prod/*"
      ]
    }
  ]
}
```

#### AWS CLI로 정책 생성

```bash
# 정책 생성
aws iam create-policy \
  --policy-name KoSpotS3AccessPolicy \
  --policy-document file://s3-access-policy.json

# 정책을 EC2 역할에 연결
aws iam attach-role-policy \
  --role-name KoSpotEC2CodeDeployRole \
  --policy-arn arn:aws:iam::YOUR_ACCOUNT_ID:policy/KoSpotS3AccessPolicy
```

### 2. GitHub Actions용 IAM 사용자

GitHub Actions에서 배포 버킷에 업로드할 수 있도록 IAM 사용자를 생성합니다.

#### 정책 생성

**정책 이름**: `KoSpotGitHubActionsPolicy`

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3DeployBucketAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::kospot-deploy-bucket",
        "arn:aws:s3:::kospot-deploy-bucket/*"
      ]
    },
    {
      "Sid": "CodeDeployAccess",
      "Effect": "Allow",
      "Action": [
        "codedeploy:CreateDeployment",
        "codedeploy:GetDeployment",
        "codedeploy:GetDeploymentConfig",
        "codedeploy:GetApplicationRevision",
        "codedeploy:RegisterApplicationRevision"
      ],
      "Resource": "*"
    }
  ]
}
```

#### IAM 사용자 생성 및 권한 부여

```bash
# IAM 사용자 생성
aws iam create-user --user-name kospot-github-actions

# 정책 생성
aws iam create-policy \
  --policy-name KoSpotGitHubActionsPolicy \
  --policy-document file://github-actions-policy.json

# 정책 연결
aws iam attach-user-policy \
  --user-name kospot-github-actions \
  --policy-arn arn:aws:iam::YOUR_ACCOUNT_ID:policy/KoSpotGitHubActionsPolicy

# 액세스 키 생성
aws iam create-access-key --user-name kospot-github-actions
```

⚠️ 생성된 **Access Key ID**와 **Secret Access Key**를 GitHub Secrets에 저장하세요:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

---

## 🔗 애플리케이션 연동

### 1. Spring Boot 설정

#### application.yml

```yaml
cloud:
  aws:
    credentials:
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
    region:
      static: ${AWS_REGION}
    s3:
      bucket: ${S3_BUCKET}  # kospot-images-prod
```

### 2. S3 클라이언트 설정 클래스 (예시)

```java
@Configuration
public class S3Config {
    
    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;
    
    @Value("${cloud.aws.region.static}")
    private String region;
    
    @Bean
    public AmazonS3 amazonS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }
}
```

### 3. 이미지 업로드 서비스 (예시)

```java
@Service
@RequiredArgsConstructor
public class S3ImageService {
    
    private final AmazonS3 amazonS3;
    
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    
    /**
     * 이미지를 S3에 업로드하고 URL 반환
     */
    public String uploadImage(MultipartFile file, String category) throws IOException {
        // 파일명 생성 (UUID + 원본 확장자)
        String fileName = generateFileName(file.getOriginalFilename());
        String s3Key = category + "/" + fileName;
        
        // S3에 업로드
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        
        amazonS3.putObject(
            new PutObjectRequest(bucket, s3Key, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.Private)
        );
        
        // Presigned URL 생성 (24시간 유효)
        return generatePresignedUrl(s3Key);
    }
    
    /**
     * Presigned URL 생성
     */
    public String generatePresignedUrl(String s3Key) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60 * 24; // 24시간
        expiration.setTime(expTimeMillis);
        
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
            new GeneratePresignedUrlRequest(bucket, s3Key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);
        
        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }
    
    /**
     * S3에서 이미지 삭제
     */
    public void deleteImage(String s3Key) {
        amazonS3.deleteObject(bucket, s3Key);
    }
    
    private String generateFileName(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID().toString() + extension;
    }
}
```

### 4. 환경변수 설정

#### EC2의 .env 파일

```bash
# AWS Configuration
AWS_ACCESS_KEY=your-aws-access-key-id
AWS_SECRET_KEY=your-aws-secret-access-key
AWS_REGION=ap-northeast-2

# S3 Buckets
S3_BUCKET=kospot-images-prod
```

---

## ✅ 설정 검증

### 1. 버킷 접근 테스트 (AWS CLI)

```bash
# CodeDeploy 버킷 확인
aws s3 ls s3://kospot-deploy-bucket

# 이미지 버킷 확인
aws s3 ls s3://kospot-images-prod

# 테스트 파일 업로드
echo "test" > test.txt
aws s3 cp test.txt s3://kospot-images-prod/test.txt

# 테스트 파일 다운로드
aws s3 cp s3://kospot-images-prod/test.txt ./downloaded-test.txt

# 테스트 파일 삭제
aws s3 rm s3://kospot-images-prod/test.txt
```

### 2. EC2에서 접근 테스트

```bash
# EC2 SSH 접속
ssh -i your-key.pem ubuntu@your-ec2-ip

# AWS CLI 설치 확인
aws --version

# 버킷 접근 확인
aws s3 ls s3://kospot-images-prod

# IAM 역할 확인
aws sts get-caller-identity
```

### 3. 애플리케이션에서 접근 테스트

Spring Boot 애플리케이션 시작 후:

```bash
# 로그 확인
docker-compose logs app | grep S3

# 헬스체크
curl http://localhost:8080/actuator/health
```

---

## 📊 모니터링 및 비용 최적화

### 1. CloudWatch 메트릭 설정

```bash
# S3 버킷 메트릭 활성화
aws s3api put-bucket-metrics-configuration \
  --bucket kospot-images-prod \
  --id EntireBucket \
  --metrics-configuration Id=EntireBucket
```

### 2. 비용 최적화 팁

1. **수명 주기 정책**: 오래된 버전을 Glacier로 이동
2. **Intelligent-Tiering**: 자주 사용하지 않는 객체 자동 이동
3. **압축**: 이미지 최적화 후 업로드
4. **CloudFront**: CDN을 통한 전송 비용 절감

### 3. 버킷 크기 모니터링

```bash
# 버킷 크기 확인
aws s3 ls s3://kospot-images-prod --recursive --human-readable --summarize
```

---

## 🔒 보안 권장사항

1. ✅ **퍼블릭 액세스 차단**: 모든 버킷에서 퍼블릭 액세스 차단
2. ✅ **Presigned URL 사용**: 임시 URL로 이미지 제공
3. ✅ **IAM 역할 사용**: EC2에서는 액세스 키 대신 IAM 역할 사용
4. ✅ **암호화 활성화**: SSE-S3 또는 SSE-KMS
5. ✅ **버저닝 활성화**: 중요 이미지 복구 가능
6. ✅ **MFA Delete**: 중요 버킷에 MFA 삭제 보호
7. ✅ **접근 로그**: S3 액세스 로그 활성화

---

## 📚 추가 리소스

- [AWS S3 User Guide](https://docs.aws.amazon.com/s3/)
- [S3 Presigned URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)
- [S3 Best Practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/security-best-practices.html)

---

**작성일:** 2025-10-31  
**버전:** 1.0.0  
**프로젝트:** KoSpot Backend


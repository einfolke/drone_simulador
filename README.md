
# 🛸 Drone Simulator

<img src= "img/front-react.gif">

Simulador de entregas por drones em áreas urbanas, com gerenciamento de rotas, capacidade, distância e prioridade de entrega.

## 📋 Descrição
O **Drone Delivery Simulator** é um sistema que permite:
- Cadastrar drones e entregas.
- Simular voos considerando **peso máximo**, **distância limite** e **prioridade**.
- Exibir a entrada e saída de dados em uma interface web.
- Integrar API em **Node.js**/**Java** com frontend em **React**.

## ✈️ Funcionalidades Avançadas

Além das opções básicas de cadastro e simulação de rotas, o projeto conta com recursos adicionais que tornam a experiência mais realista e desafiadora:

- Simulação de Bateria – a carga do drone é reduzida conforme o tempo de voo ou distância percorrida, exigindo planejamento eficiente de cada entrega.

- Obstáculos e Zonas de Exclusão Aérea – é possível definir áreas proibidas de sobrevoo, fazendo o algoritmo recalcular trajetos seguros.

- Cálculo de Tempo Total de Entrega – o sistema estima o tempo necessário para concluir cada rota, permitindo acompanhar prazos e desempenho.

- Otimização Inteligente – as rotas são calculadas considerando peso da carga, prioridade da entrega e distância, equilibrando eficiência e segurança.

## Motivação da escolha React + Vite

A combinação React + Vite foi adotada porque:

React oferece flexibilidade na construção de interfaces dinâmicas com componentes reutilizáveis.

Vite fornece build extremamente rápido, recarregamento instantâneo (HMR) e uma configuração leve.

Juntas, essas tecnologias aumentam a produtividade de desenvolvimento e melhoram a experiência de prototipação rápida.

## ⚡ Tecnologias
| Camada | Tecnologia |
|--------|------------|
| Backend | **Java** (motor de simulação) + **Node.js** (API REST) |
| Frontend | **React + Vite** |
| Banco de Dados | PostgreSQL (com Flyway para migrações) |
| Outros | Docker, Maven, npm |

## 🏗️ Estrutura de Pastas
```bash
drone_simulador/
├─ .idea/
├─ .mvn/
├─ .vscode/
├─ client/
├─ demo/
├─ node/
├─ src/
├─ .env
├─ .gitattributes
├─ docker-compose.yml
├─ package-lock.json
├─ package.json
├─ pom.xml
├─ README.md
└─ schema.sql

```
## 🚀 Como Rodar o Projeto

### 1️⃣ Pré-requisitos
- **Java 17**  
- **Node.js 22.20.0 e npm 10.9.3**  
- **PostgreSQL** (local ou via Docker)  
- **Maven** (para compilar o módulo Java)

### 2️⃣ Clonar o repositório
```bash
git clone https://github.com/einfolke/drone_simulador
cd drone_simulador
```

### 3️⃣ Banco de Dados
Crie um banco `drone_simulador` no PostgreSQL e ajuste as credenciais em  
`node/.env` ou `java/src/config.properties` (separado, conforme a sua arquitetura).  
Depois execute:
```bash
mvn flyway:migrate
```

### 4️⃣ Backend
- **Java**  
  ```bash
  cd java
  mvn clean package
  java -jar target/drone-simulador.jar
  ```
- **Node.js**  
  ```bash
  cd node
  npm install
  npm run dev
  ```
## 5️⃣Banco de dados
Execute os scripts SQL de inicialização
```env
psql -U postgres -d drone_simulador < schema.sql
```
### 6️⃣ Frontend
```bash
cd client
npm install
npm run dev
```
Acesse: **http://localhost:5173**

## 🔑 Variáveis de Ambiente
Exemplo de `.env` para Node:
```env
PORT=4000
DB_HOST=localhost
DB_USER=user
DB_PASS=password
DB_NAME=drones
```


## 🧪 Testes
Node.js:
```bash
cd node
npm test
```
Java (JUnit):
```bash
cd java
mvn test
```

## 📜 Licença
Este projeto é distribuído sob a licença MIT – veja o arquivo [LICENSE](LICENSE) para mais detalhes.

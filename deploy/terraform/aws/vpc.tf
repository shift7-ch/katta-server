resource "aws_vpc" "katta" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name        = "${terraform.workspace}-vpc"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_vpc_endpoint" "ecr_api" {
  vpc_id              = aws_vpc.katta.id
  service_name        = "com.amazonaws.${var.region}.ecr.api"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoint_sg.id]
  private_dns_enabled = true

  tags = {
    Name        = "${terraform.workspace}-ecr-api-endpoint"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_vpc_endpoint" "ecr_dkr" {
  vpc_id              = aws_vpc.katta.id
  service_name        = "com.amazonaws.${var.region}.ecr.dkr"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoint_sg.id]
  private_dns_enabled = true

  tags = {
    Name        = "${terraform.workspace}-ecr-dkr-endpoint"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_vpc_endpoint" "ecr_logs" {
  vpc_id              = aws_vpc.katta.id
  service_name        = "com.amazonaws.${var.region}.logs"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoint_sg.id]
  private_dns_enabled = true

  tags = {
    Name        = "${terraform.workspace}-logs-endpoint"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_vpc_endpoint" "ecr_secretsmanager" {
  vpc_id              = aws_vpc.katta.id
  service_name        = "com.amazonaws.${var.region}.secretsmanager"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private[*].id
  security_group_ids  = [aws_security_group.vpc_endpoint_sg.id]
  private_dns_enabled = true

  tags = {
    Name        = "${terraform.workspace}-secretsmanager-endpoint"
    Project     = var.project
    Environment = terraform.workspace
  }
}


resource "aws_subnet" "public" {
  count = length(data.aws_availability_zones.available.names)

  vpc_id                  = aws_vpc.katta.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 4, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name        = "${terraform.workspace}-public-${count.index}"
    Project     = var.project
    Environment = terraform.workspace
    Type        = "public"
  }
}

resource "aws_internet_gateway" "public_igw" {
  vpc_id = aws_vpc.katta.id

  tags = {
    Name        = "${terraform.workspace}-internet-gateway"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_subnet" "private" {
  count = length(data.aws_availability_zones.available.names)

  vpc_id            = aws_vpc.katta.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 4, count.index + length(data.aws_availability_zones.available.names))
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name        = "${terraform.workspace}-private-${count.index}"
    Project     = var.project
    Environment = terraform.workspace
    Type        = "private"
  }
}

resource "aws_eip" "nat" {
  tags = {
    Name        = "${terraform.workspace}-nat-eip"
    Project     = var.project
    Environment = terraform.workspace
  }
}
resource "aws_nat_gateway" "private_nat" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id
  tags = {
    Name        = "${terraform.workspace}-nat-gateway"
    Project     = var.project
    Environment = terraform.workspace
  }
}
resource "aws_route_table" "private_subnet" {
  vpc_id = aws_vpc.katta.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.private_nat.id
  }

  tags = {
    Name        = "${terraform.workspace}-private-table"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_route_table_association" "private_subnets" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private_subnet.id
}

resource "aws_route_table" "public_subnet" {
  vpc_id = aws_vpc.katta.id

  tags = {
    Name        = "${terraform.workspace}-public-table"
    Project     = var.project
    Environment = terraform.workspace
  }
}

resource "aws_route" "internet_access" {
  route_table_id         = aws_route_table.public_subnet.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.public_igw.id
}

resource "aws_route_table_association" "public_subnets" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public_subnet.id
}

resource "aws_db_subnet_group" "private_subnet_group" {
  name        = "${terraform.workspace}-private-subnet-group"
  description = "Private subnet group"
  subnet_ids  = aws_subnet.private[*].id

  tags = {
    Name        = "${terraform.workspace}-private-subnet-group"
    Project     = var.project
    Environment = terraform.workspace
    Type        = "private"
  }
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  fullName: string;
  role: string;
  departmentId?: number;
  departmentName?: string;
  permissions: string[];
  profilePictureUrl?: string;
}

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  fullName?: string;
  role: string;
  departmentId?: number;
  departmentName?: string;
  isActive: boolean;
  lastLogin?: string;
  permissions?: string[];
  phoneNumber?: string;
  address?: string;
  profilePictureUrl?: string;
  jobPositionId?: number;
  jobPositionName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface JobPosition {
  id: number;
  name: string;
  departmentId: number;
}

export interface Department {
  id: number;
  name: string;
  description: string;
  parentId?: number;
  parentName?: string;
  jobPositions?: JobPosition[];
}

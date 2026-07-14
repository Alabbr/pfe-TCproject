export interface UserBasicInfo {
  id: number;
  firstName: string;
  lastName: string;
  email?: string;
}

export interface Project {
  id: number;
  title: string;
  description: string;
  status: string;
  startDate: string;
  deadline: string;
  createdBy: UserBasicInfo;
  members: UserBasicInfo[];
}

export interface Task {
  id: number;
  title: string;
  description: string;
  status: 'A_FAIRE' | 'EN_COURS' | 'EN_VALIDATION' | 'TERMINE';
  priority: 'BASSE' | 'NORMALE' | 'HAUTE' | 'URGENTE';
  deadline: string;
  projectId: number;
  assignee?: UserBasicInfo;
  reporter: UserBasicInfo;
}

export interface TaskHistory {
  id: number;
  oldStatus: string;
  newStatus: string;
  comment: string;
  changedAt: string;
  changedBy: UserBasicInfo;
}

export interface ProjectDocument {
  id: number;
  fileName: string;
  fileType: string;
  uploadedAt: string;
  uploadedBy: UserBasicInfo;
}

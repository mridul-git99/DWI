export enum EffectType {
  SQL_QUERY = 'SQL_QUERY',
  MONGO_QUERY = 'MONGO_QUERY',
  REST_API = 'REST_API',
}

export enum MethodType {
  GET = 'GET',
  POST = 'POST',
  PATCH = 'PATCH',
  PUT = 'PUT',
}

export enum TriggerType {
  START_TASK = 'START_TASK',
  COMPLETE_TASK = 'COMPLETE_TASK',
}

export enum EffectEntity {
  parameter = 'parameter',
  task = 'task',
  effect = 'effect',
  constant = 'constant',
}

export enum MentionNodeType {
  text = 'text',
  paragraph = 'paragraph',
  root = 'root',
  custom_beautifulMention = 'custom-beautifulMention',
}

export enum MentionConstantType {
  jobId = 'jobId',
  facilityId = 'facilityId',
  useCaseId = 'useCaseId',
}

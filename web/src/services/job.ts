import { Job, JobRun, JobCreateRequest, JobUpdateRequest, JobQueryParams, JobRunQueryParams, PaginatedResponse } from '../types/job';

// Mock data for jobs
const mockJobs: Job[] = [
  {
    id: '1',
    name: '每日文档处理任务',
    description: '自动处理上传的文档并建立索引',
    job_type: 'document_processing',
    status: 'enabled',
    schedule: '0 2 * * *', // Daily at 2 AM
    config: {
      knowledge_base_id: '1',
      max_documents: 100,
      processing_timeout: 3600
    },
    last_run_at: '2024-01-19T02:00:00Z',
    next_run_at: '2024-01-20T02:00:00Z',
    run_count: 15,
    success_count: 14,
    failure_count: 1,
    created_by: 'admin',
    created_at: '2024-01-01T00:00:00Z',
    updated_at: '2024-01-19T02:00:00Z'
  },
  {
    id: '2',
    name: '智能客服Agent',
    description: '定时运行的客户服务Agent',
    job_type: 'agent',
    status: 'enabled',
    schedule: '*/30 * * * *', // Every 30 minutes
    config: {
      agent_id: '1',
      query_limit: 50,
      response_timeout: 300
    },
    last_run_at: '2024-01-19T23:30:00Z',
    next_run_at: '2024-01-20T00:00:00Z',
    run_count: 48,
    success_count: 45,
    failure_count: 3,
    created_by: 'user1',
    created_at: '2024-01-05T10:00:00Z',
    updated_at: '2024-01-19T23:30:00Z'
  },
  {
    id: '3',
    name: '数据同步工作流',
    description: '同步外部数据源到知识库',
    job_type: 'workflow',
    status: 'disabled',
    schedule: '0 */6 * * *', // Every 6 hours
    config: {
      workflow_id: '1',
      data_source: 'external_api',
      sync_batch_size: 1000
    },
    last_run_at: '2024-01-18T12:00:00Z',
    next_run_at: null,
    run_count: 12,
    success_count: 10,
    failure_count: 2,
    created_by: 'admin',
    created_at: '2024-01-10T08:00:00Z',
    updated_at: '2024-01-18T12:00:00Z'
  }
];

// Mock data for job runs
const mockJobRuns: JobRun[] = [
  {
    id: 'run_1',
    job_id: '1',
    job_name: '每日文档处理任务',
    status: 'completed',
    start_time: '2024-01-19T02:00:00Z',
    end_time: '2024-01-19T02:15:30Z',
    duration: 930, // 15.5 minutes
    logs: 'Starting document processing...\nProcessing 15 documents...\nCompleted successfully',
    result: {
      processed_documents: 15,
      failed_documents: 0,
      new_chunks: 125
    },
    created_at: '2024-01-19T02:00:00Z',
    updated_at: '2024-01-19T02:15:30Z'
  },
  {
    id: 'run_2',
    job_id: '1',
    job_name: '每日文档处理任务',
    status: 'failed',
    start_time: '2024-01-18T02:00:00Z',
    end_time: '2024-01-18T02:05:15Z',
    duration: 315, // 5.25 minutes
    logs: 'Starting document processing...\nError: Connection timeout to document store',
    error_message: 'Failed to connect to document storage service',
    created_at: '2024-01-18T02:00:00Z',
    updated_at: '2024-01-18T02:05:15Z'
  },
  {
    id: 'run_3',
    job_id: '2',
    job_name: '智能客服Agent',
    status: 'running',
    start_time: '2024-01-19T23:30:00Z',
    duration: 180, // 3 minutes so far
    logs: 'Agent initialized...\nProcessing customer queries...\n25 queries processed',
    created_at: '2024-01-19T23:30:00Z',
    updated_at: '2024-01-19T23:33:00Z'
  }
];

export const jobService = {
  // Get jobs list with pagination and filtering
  async getJobs(params: JobQueryParams): Promise<PaginatedResponse<Job>> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    let filteredJobs = [...mockJobs];
    
    // Apply filters
    if (params.name) {
      filteredJobs = filteredJobs.filter(job => 
        job.name.toLowerCase().includes(params.name.toLowerCase())
      );
    }
    
    if (params.job_type) {
      filteredJobs = filteredJobs.filter(job => job.job_type === params.job_type);
    }
    
    if (params.status) {
      filteredJobs = filteredJobs.filter(job => job.status === params.status);
    }
    
    if (params.created_by) {
      filteredJobs = filteredJobs.filter(job => job.created_by === params.created_by);
    }
    
    // Apply pagination
    const start = (params.page - 1) * params.pageSize;
    const end = start + params.pageSize;
    const paginatedJobs = filteredJobs.slice(start, end);
    
    return {
      data: paginatedJobs,
      total: filteredJobs.length,
      page: params.page,
      pageSize: params.pageSize
    };
  },

  // Get job by ID
  async getJobById(id: string): Promise<Job> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    const job = mockJobs.find(job => job.id === id);
    if (!job) {
      throw new Error('Job not found');
    }
    
    return job;
  },

  // Create new job
  async createJob(data: JobCreateRequest): Promise<Job> {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const newJob: Job = {
      id: String(Date.now()),
      ...data,
      status: 'enabled',
      run_count: 0,
      success_count: 0,
      failure_count: 0,
      created_by: 'current_user', // 待确认：需要从auth获取当前用户
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    mockJobs.unshift(newJob);
    return newJob;
  },

  // Update existing job
  async updateJob(id: string, data: JobUpdateRequest): Promise<Job> {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const jobIndex = mockJobs.findIndex(job => job.id === id);
    if (jobIndex === -1) {
      throw new Error('Job not found');
    }
    
    const updatedJob = {
      ...mockJobs[jobIndex],
      ...data,
      updated_at: new Date().toISOString()
    };
    
    mockJobs[jobIndex] = updatedJob;
    return updatedJob;
  },

  // Delete job
  async deleteJob(id: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const jobIndex = mockJobs.findIndex(job => job.id === id);
    if (jobIndex === -1) {
      throw new Error('Job not found');
    }
    
    mockJobs.splice(jobIndex, 1);
  },

  // Run job immediately
  async runJob(id: string): Promise<JobRun> {
    await new Promise(resolve => setTimeout(resolve, 500));
    
    const job = mockJobs.find(job => job.id === id);
    if (!job) {
      throw new Error('Job not found');
    }
    
    // Create a new job run
    const newRun: JobRun = {
      id: `run_${Date.now()}`,
      job_id: id,
      job_name: job.name,
      status: 'running',
      start_time: new Date().toISOString(),
      logs: 'Job started manually...',
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    mockJobRuns.unshift(newRun);
    
    // Update job status
    job.last_run_at = new Date().toISOString();
    job.run_count++;
    
    // Simulate job completion after a delay
    setTimeout(() => {
      const runIndex = mockJobRuns.findIndex(run => run.id === newRun.id);
      if (runIndex !== -1) {
        mockJobRuns[runIndex].status = 'completed';
        mockJobRuns[runIndex].end_time = new Date().toISOString();
        mockJobRuns[runIndex].duration = 300; // 5 minutes
        mockJobRuns[runIndex].logs += '\nJob completed successfully';
        mockJobRuns[runIndex].updated_at = new Date().toISOString();
        
        job.success_count++;
      }
    }, 3000);
    
    return newRun;
  },

  // Get job runs with pagination and filtering
  async getJobRuns(params: JobRunQueryParams): Promise<PaginatedResponse<JobRun>> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    let filteredRuns = [...mockJobRuns];
    
    if (params.job_id) {
      filteredRuns = filteredRuns.filter(run => run.job_id === params.job_id);
    }
    
    if (params.status) {
      filteredRuns = filteredRuns.filter(run => run.status === params.status);
    }
    
    if (params.start_time_from) {
      filteredRuns = filteredRuns.filter(run => 
        run.start_time && new Date(run.start_time) >= new Date(params.start_time_from!)
      );
    }
    
    if (params.start_time_to) {
      filteredRuns = filteredRuns.filter(run => 
        run.start_time && new Date(run.start_time) <= new Date(params.start_time_to!)
      );
    }
    
    // Apply pagination
    const start = (params.page - 1) * params.pageSize;
    const end = start + params.pageSize;
    const paginatedRuns = filteredRuns.slice(start, end);
    
    return {
      data: paginatedRuns,
      total: filteredRuns.length,
      page: params.page,
      pageSize: params.pageSize
    };
  },

  // Get job run by ID
  async getJobRunById(id: string): Promise<JobRun> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    const run = mockJobRuns.find(run => run.id === id);
    if (!run) {
      throw new Error('Job run not found');
    }
    
    return run;
  },

  // Cancel running job
  async cancelJobRun(id: string): Promise<JobRun> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const run = mockJobRuns.find(run => run.id === id);
    if (!run) {
      throw new Error('Job run not found');
    }
    
    if (run.status !== 'running') {
      throw new Error('Job is not running');
    }
    
    run.status = 'cancelled';
    run.end_time = new Date().toISOString();
    run.duration = run.start_time ? 
      Math.floor((new Date().getTime() - new Date(run.start_time).getTime()) / 1000) : 0;
    run.logs += '\nJob cancelled by user';
    run.updated_at = new Date().toISOString();
    
    return run;
  }
};
package com.armandorvila.poc.price.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;

import com.armandorvila.poc.price.domain.Batch;
import com.armandorvila.poc.price.domain.BatchState;
import com.armandorvila.poc.price.repository.BatchRepository;

public class BatchJobExecutionListenerTests {

	private BatchJobExecutionListener jobExecutionListener;

	private BatchRepository batchRepository;

	private JobExecution jobExecution;

	private Batch testBatch;

	@Before
	public void setUp() {
		batchRepository = mock(BatchRepository.class);
		jobExecutionListener = new BatchJobExecutionListener(batchRepository);

		testBatch = new Batch("1234", "dataFile=mydata.csv", BatchState.IN_PROGRESS, null, null);
		jobExecution = MetaDataInstanceFactory.createJobExecution("loadPrices", 1L, 1L, testBatch.getDataFile());
	}

	@Test
	public void should_CreateBatch_Before_JobExecution() {
		given(batchRepository.save(any(Batch.class))).willReturn(testBatch);

		jobExecutionListener.beforeJob(jobExecution);

		then(batchRepository).should(times(1)).save(any(Batch.class));

		assertThat(jobExecution.getExecutionContext().get(Batch.class.getSimpleName())).isEqualTo(testBatch.getId());
	}
	
	@Test
	public void should_UpdateBatch_When_JobExecutionCompleted() {
		jobExecution.getExecutionContext().putString(Batch.class.getSimpleName(), testBatch.getId());
		jobExecution.setStatus(BatchStatus.COMPLETED);
		
		given(batchRepository.findById(testBatch.getId())).willReturn(Optional.of(testBatch));
		given(batchRepository.save(any(Batch.class))).willReturn(testBatch);
		

		jobExecutionListener.afterJob(jobExecution);

		then(batchRepository).should(times(1)).findById(testBatch.getId());
		then(batchRepository).should(times(1)).save(any(Batch.class));
	}
	
	@Test
	public void should_NotUpdateBatch_When_JobExecutionNotCompleted() {
		jobExecution.getExecutionContext().putString(Batch.class.getSimpleName(), testBatch.getId());
		jobExecution.setStatus(BatchStatus.FAILED);

		jobExecutionListener.afterJob(jobExecution);

		then(batchRepository).should(times(0)).findById(testBatch.getId());
		then(batchRepository).should(times(0)).save(any(Batch.class));
	}
}
